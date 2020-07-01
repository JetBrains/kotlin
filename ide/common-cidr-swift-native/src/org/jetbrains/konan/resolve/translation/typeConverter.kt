package org.jetbrains.konan.resolve.translation

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayUtil
import com.jetbrains.cidr.lang.psi.OCTypeElement
import com.jetbrains.cidr.lang.symbols.OCQualifiedName
import com.jetbrains.cidr.lang.symbols.OCQualifiedNameWithArguments
import com.jetbrains.cidr.lang.symbols.OCSymbolReference
import com.jetbrains.cidr.lang.symbols.cpp.OCSymbolWithQualifiedName
import com.jetbrains.cidr.lang.symbols.objc.OCMethodSymbol
import com.jetbrains.cidr.lang.types.*
import com.jetbrains.cidr.lang.util.OCElementFactory
import org.jetbrains.kotlin.backend.konan.objcexport.*

//todo provide virtual files
fun ObjCType.toOCType(project: Project, context: OCSymbolWithQualifiedName?, file: VirtualFile? = null): OCType {
    return TypeBuilder(project, context, file, true).build(this, false)
}

private class TypeBuilder(
    private val project: Project,
    private val context: OCSymbolWithQualifiedName?,
    private val file: VirtualFile?,
    private val assumeNonNull: Boolean
) {
    fun build(objCType: ObjCType, nullability: Boolean = false): OCType = when (objCType) {
        is ObjCRawType -> {
            //todo improve
            val codeFragment = OCElementFactory.typeCodeFragment(objCType.rawText, project, null, false, false)
            val typeElement = PsiTreeUtil.getChildOfType(codeFragment, OCTypeElement::class.java)!!
            typeElement.rawType
        }

        is ObjCClassType ->
            OCPointerType.to(
                referenceType(objCType.className, objCType.typeArguments.map { arg -> build(arg) }, false),
                null, null, nullability.toOCNullability(), false, false
            )

        is ObjCProtocolType -> {
            val ref = OCSymbolReference.getDummyGlobalReference(OCQualifiedName.interned("id"))
            OCReferenceTypeBuilder(ref)
                .setSingleProtocolName(objCType.protocolName)
                .setNullability(nullability.toOCNullability())
                .build()
        }

        ObjCIdType -> referenceType("id", emptyList(), nullability)

        ObjCInstanceType -> referenceType(OCMethodSymbol.INSTANCETYPE, emptyList(), nullability)

        is ObjCBlockPointerType -> {
            val returnType = build(objCType.returnType)
            val parameterTypes = objCType.parameterTypes.map { p -> build(p) }
            val functionType = OCFunctionType(returnType, parameterTypes, null)
            OCBlockPointerType(functionType, null, nullability.toOCNullability(), false, false)
        }

        is ObjCNullableReferenceType -> build(objCType.nonNullType, true)

        is ObjCPrimitiveType -> referenceType(objCType.cName, emptyList(), false)

        is ObjCPointerType -> OCPointerType.to(build(objCType.pointee), null, null, objCType.nullable.toOCNullability(), false, false)

        ObjCVoidType -> OCVoidType.instance()
        is ObjCGenericTypeDeclaration -> OCUnknownType.INSTANCE //todo
        ObjCMetaClassType -> referenceType("Class", emptyList(), nullability)
    }

    private fun Boolean.toOCNullability(): OCNullability = when (this) {
        true -> OCNullability.NULLABLE
        false -> when {
            assumeNonNull -> OCNullability.NONNULL
            else -> OCNullability.UNSPECIFIED
        }
    }

    private fun referenceType(refName: String, arguments: List<OCType>, nullability: Boolean): OCReferenceType {
        val qname: OCQualifiedName = if (arguments.isEmpty()) {
            OCQualifiedName.interned(refName)
        } else {
            OCQualifiedNameWithArguments(null, refName, arguments)
        }

        val ref: OCSymbolReference = OCSymbolReference.getGlobalReference(qname, context, file, -1)

        val refBuilder = OCReferenceTypeBuilder(ref)
        refBuilder.setNullability(nullability.toOCNullability())
        return refBuilder.build()
    }
}

fun createSuperType(superClass: String?, superProtocols: List<String>?): OCReferenceType {
    return OCReferenceType.fromText(superClass ?: "", superProtocols?.toTypedArray() ?: ArrayUtil.EMPTY_STRING_ARRAY)
}