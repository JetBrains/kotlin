/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.ir.allOverridden
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensions
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.lower.MultifileFacadeFileEntry
import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.FrameMapBase
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.SourceInfo
import org.jetbrains.kotlin.codegen.classFileContainsMethod
import org.jetbrains.kotlin.codegen.inline.SourceMapper
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.inline.INLINE_ONLY_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmClassSignature
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class IrFrameMap : FrameMapBase<IrSymbol>() {
    private val typeMap = mutableMapOf<IrSymbol, Type>()

    override fun enter(key: IrSymbol, type: Type): Int {
        typeMap[key] = type
        return super.enter(key, type)
    }

    override fun leave(key: IrSymbol): Int {
        typeMap.remove(key)
        return super.leave(key)
    }

    fun typeOf(symbol: IrSymbol): Type = typeMap[symbol]
        ?: error("No mapping for symbol: ${symbol.owner.render()}")
}

internal val IrFunction.isStatic
    get() = (this.dispatchReceiverParameter == null && this !is IrConstructor)

fun IrFrameMap.enter(irDeclaration: IrSymbolOwner, type: Type): Int {
    return enter(irDeclaration.symbol, type)
}

fun IrFrameMap.leave(irDeclaration: IrSymbolOwner): Int {
    return leave(irDeclaration.symbol)
}

val IrClass.isJvmInterface get() = isAnnotationClass || isInterface

val IrDeclaration.fileParent: IrFile
    get() {
        return when (val myParent = parent) {
            is IrFile -> myParent
            else -> (myParent as IrDeclaration).fileParent
        }
    }

internal val DeclarationDescriptorWithSource.psiElement: PsiElement?
    get() = (source as? PsiSourceElement)?.psi

fun JvmBackendContext.getSourceMapper(declaration: IrClass): SourceMapper {
    val sourceManager = this.psiSourceManager
    val fileEntry = sourceManager.getFileEntry(declaration.fileParent)
    // NOTE: apparently inliner requires the source range to cover the
    //       whole file the class is declared in rather than the class only.
    val endLineNumber = when (fileEntry) {
        is MultifileFacadeFileEntry -> 0
        else -> fileEntry?.getSourceRangeInfo(0, fileEntry.maxOffset)?.endLineNumber ?: 0
    }
    val sourceFileName = when (fileEntry) {
        is MultifileFacadeFileEntry -> fileEntry.partFiles.singleOrNull()?.name
        else -> declaration.fileParent.name
    }
    return SourceMapper(
        SourceInfo(
            sourceFileName,
            typeMapper.mapClass(declaration).internalName,
            endLineNumber + 1
        )
    )
}

val IrType.isExtensionFunctionType: Boolean
    get() = isFunctionTypeOrSubtype() && hasAnnotation(FqNames.extensionFunctionType)


/* Borrowed with modifications from AsmUtil.java */

private val NO_FLAG_LOCAL = 0

private fun IrDeclaration.getVisibilityAccessFlagForAnonymous(): Int =
    if (isInlineOrContainedInInline(parent as? IrDeclaration)) Opcodes.ACC_PUBLIC else AsmUtil.NO_FLAG_PACKAGE_PRIVATE

fun IrClass.calculateInnerClassAccessFlags(context: JvmBackendContext): Int {
    val isLambda = superTypes.any {
        it.safeAs<IrSimpleType>()?.classifier === context.ir.symbols.lambdaClass
    }
    val visibility = when {
        isLambda -> getVisibilityAccessFlagForAnonymous()
        visibility === DescriptorVisibilities.LOCAL -> Opcodes.ACC_PUBLIC
        else -> getVisibilityAccessFlag()
    }
    return visibility or
            if (origin.isSynthetic) Opcodes.ACC_SYNTHETIC else 0 or
                    innerAccessFlagsForModalityAndKind() or
                    if (isInner) 0 else Opcodes.ACC_STATIC
}

private fun IrClass.innerAccessFlagsForModalityAndKind(): Int {
    when (kind) {
        ClassKind.INTERFACE -> return Opcodes.ACC_ABSTRACT or Opcodes.ACC_INTERFACE
        ClassKind.ENUM_CLASS -> return Opcodes.ACC_FINAL or Opcodes.ACC_ENUM
        ClassKind.ANNOTATION_CLASS -> return Opcodes.ACC_ABSTRACT or Opcodes.ACC_ANNOTATION or Opcodes.ACC_INTERFACE
        else -> {
            if (modality === Modality.FINAL) {
                return Opcodes.ACC_FINAL
            } else if (modality === Modality.ABSTRACT || modality === Modality.SEALED) {
                return Opcodes.ACC_ABSTRACT
            }
        }
    }
    return 0
}

fun IrDeclarationWithVisibility.getVisibilityAccessFlag(kind: OwnerKind? = null): Int {
    specialCaseVisibility(kind)?.let {
        return it
    }
    return when (visibility) {
        DescriptorVisibilities.PRIVATE -> Opcodes.ACC_PRIVATE
        DescriptorVisibilities.PRIVATE_TO_THIS -> Opcodes.ACC_PRIVATE
        DescriptorVisibilities.PROTECTED -> Opcodes.ACC_PROTECTED
        JavaDescriptorVisibilities.PROTECTED_STATIC_VISIBILITY -> Opcodes.ACC_PROTECTED
        JavaDescriptorVisibilities.PROTECTED_AND_PACKAGE -> Opcodes.ACC_PROTECTED
        DescriptorVisibilities.PUBLIC -> Opcodes.ACC_PUBLIC
        DescriptorVisibilities.INTERNAL -> Opcodes.ACC_PUBLIC
        DescriptorVisibilities.LOCAL -> NO_FLAG_LOCAL
        JavaDescriptorVisibilities.PACKAGE_VISIBILITY -> AsmUtil.NO_FLAG_PACKAGE_PRIVATE
        else -> throw IllegalStateException("$visibility is not a valid visibility in backend for ${ir2string(this)}")
    }
}

private fun IrDeclarationWithVisibility.specialCaseVisibility(kind: OwnerKind?): Int? {
    if (this is IrClass && DescriptorVisibilities.isPrivate(visibility) && isCompanion && hasInterfaceParent()) {
        // TODO: non-intrinsic
        return Opcodes.ACC_PUBLIC
    }

    if (this is IrConstructor && parentAsClass.isInline && kind === OwnerKind.IMPLEMENTATION) {
        return Opcodes.ACC_PRIVATE
    }

    if (this is IrFunction && (isReifiable() || isBridge())) {
        return Opcodes.ACC_PUBLIC
    }

    if (isEffectivelyInlineOnly()) {
        return Opcodes.ACC_PRIVATE
    }

    if (visibility === DescriptorVisibilities.LOCAL && this is IrFunction) {
        return Opcodes.ACC_PUBLIC
    }

    if (this is IrClass && this.kind === ClassKind.ENUM_ENTRY) {
        return AsmUtil.NO_FLAG_PACKAGE_PRIVATE
    }

    if (this is IrField && correspondingPropertySymbol?.owner?.isExternal == true) {
        val method = correspondingPropertySymbol?.owner?.getter ?: correspondingPropertySymbol?.owner?.setter
        ?: error("No get/set method in SyntheticJavaPropertyDescriptor: ${ir2string(correspondingPropertySymbol?.owner)}")
        return method.getVisibilityAccessFlag()
    }

    if (this is IrSimpleFunction && visibility === DescriptorVisibilities.PROTECTED &&
        allOverridden().any { it.parentAsClass.isJvmInterface }
    ) {
        return Opcodes.ACC_PUBLIC
    }

    if (!DescriptorVisibilities.isPrivate(visibility)) {
        return null
    }

    if (this is IrSimpleFunction && isSuspend) {
        return AsmUtil.NO_FLAG_PACKAGE_PRIVATE
    }

    if (this is IrConstructor && parentAsClass.kind === ClassKind.ENUM_ENTRY) {
        return AsmUtil.NO_FLAG_PACKAGE_PRIVATE
    }

    return null
}

/* Borrowed from InlineUtil. */
private tailrec fun isInlineOrContainedInInline(declaration: IrDeclaration?): Boolean = when {
    declaration === null -> false
    declaration is IrFunction && declaration.isInline -> true
    else -> isInlineOrContainedInInline(declaration.parent as? IrDeclaration)
}

/* Borrowed from inlineOnly.kt */

fun IrDeclarationWithVisibility.isInlineOnlyOrReifiable(): Boolean =
    this is IrFunction && (isReifiable() || isInlineOnly())

fun IrDeclarationWithVisibility.isEffectivelyInlineOnly(): Boolean =
    isInlineOnlyOrReifiable() || isInlineOnlyPrivateInBytecode() || isInlineOnlyPropertyAccessor()

fun IrDeclarationWithVisibility.isInlineOnlyPrivateInBytecode(): Boolean =
    (this is IrFunction && isInlineOnly()) || isPrivateInlineSuspend()

private fun IrDeclarationWithVisibility.isPrivateInlineSuspend(): Boolean =
    this is IrFunction && isSuspend && isInline && visibility == DescriptorVisibilities.PRIVATE

private fun IrDeclarationWithVisibility.isInlineOnlyPropertyAccessor(): Boolean {
    if (this !is IrSimpleFunction) return false
    val propertySymbol = correspondingPropertySymbol ?: return false
    return propertySymbol.owner.hasAnnotation(INLINE_ONLY_ANNOTATION_FQ_NAME)
}

fun IrFunction.isInlineOnly() =
    isInline && hasAnnotation(INLINE_ONLY_ANNOTATION_FQ_NAME)

fun IrFunction.isReifiable() = typeParameters.any { it.isReified }

internal fun IrFunction.isBridge() = origin == IrDeclarationOrigin.BRIDGE || origin == IrDeclarationOrigin.BRIDGE_SPECIAL

// Borrowed with modifications from ImplementationBodyCodegen.java

private val KOTLIN_MARKER_INTERFACES: Map<FqName, String> = run {
    val kotlinMarkerInterfaces = mutableMapOf<FqName, String>()
    for (platformMutabilityMapping in JavaToKotlinClassMap.mutabilityMappings) {
        kotlinMarkerInterfaces[platformMutabilityMapping.kotlinReadOnly.asSingleFqName()] = "kotlin/jvm/internal/markers/KMappedMarker"

        val mutableClassId = platformMutabilityMapping.kotlinMutable
        kotlinMarkerInterfaces[mutableClassId.asSingleFqName()] =
            "kotlin/jvm/internal/markers/K" + mutableClassId.relativeClassName.asString()
                .replace("MutableEntry", "Entry") // kotlin.jvm.internal.markers.KMutableMap.Entry for some reason
                .replace(".", "$")
    }
    kotlinMarkerInterfaces
}

internal fun IrTypeMapper.mapClassSignature(irClass: IrClass, type: Type): JvmClassSignature {
    val sw = BothSignatureWriter(BothSignatureWriter.Mode.CLASS)
    writeFormalTypeParameters(irClass.typeParameters, sw)

    sw.writeSuperclass()
    val superClassType = irClass.superTypes.find { it.getClass()?.isJvmInterface == false }
    val superClassAsmType = if (superClassType == null) {
        sw.writeClassBegin(AsmTypes.OBJECT_TYPE)
        sw.writeClassEnd()
        AsmTypes.OBJECT_TYPE
    } else {
        mapSupertype(superClassType, sw)
    }
    sw.writeSuperclassEnd()

    val superInterfaces = LinkedHashSet<String>()
    val kotlinMarkerInterfaces = LinkedHashSet<String>()
    for (superType in irClass.superTypes) {
        val superClass = superType.safeAs<IrSimpleType>()?.classifier?.safeAs<IrClassSymbol>()?.owner ?: continue
        if (superClass.isJvmInterface) {
            sw.writeInterface()
            superInterfaces.add(mapSupertype(superType, sw).internalName)
            sw.writeInterfaceEnd()
            kotlinMarkerInterfaces.addIfNotNull(KOTLIN_MARKER_INTERFACES[superClass.fqNameWhenAvailable!!])
        }
    }

    for (kotlinMarkerInterface in kotlinMarkerInterfaces) {
        sw.writeInterface()
        sw.writeAsmType(Type.getObjectType(kotlinMarkerInterface))
        sw.writeInterfaceEnd()
    }

    superInterfaces.addAll(kotlinMarkerInterfaces)

    return JvmClassSignature(
        type.internalName, superClassAsmType.internalName,
        ArrayList(superInterfaces), sw.makeJavaGenericSignature()
    )
}

/* Copied with modifications from AsmUtil.getVisibilityAccessFlagForClass */
/*
   Use this method to get visibility flag for class to define it in byte code (v.defineClass method).
   For other cases use getVisibilityAccessFlag(MemberDescriptor descriptor)
   Classes in byte code should be public or package private
*/
fun IrClass.getVisibilityAccessFlagForClass(): Int {
    /* Original had a check for SyntheticClassDescriptorForJava, never invoked in th IR backend. */
    if (kind == ClassKind.ENUM_ENTRY) {
        return AsmUtil.NO_FLAG_PACKAGE_PRIVATE
    }
    return if (visibility === DescriptorVisibilities.PUBLIC ||
        visibility === DescriptorVisibilities.PROTECTED ||
        // TODO: should be package private, but for now Kotlin's reflection can't access members of such classes
        visibility === DescriptorVisibilities.LOCAL ||
        visibility === DescriptorVisibilities.INTERNAL
    ) {
        Opcodes.ACC_PUBLIC
    } else AsmUtil.NO_FLAG_PACKAGE_PRIVATE
}

/* Borrowed and translated from ExpectedActualDeclarationChecker */
// TODO: Descriptor-based code also checks for `descriptor.isExpect`; we don't represent expect/actual distinction in IR thus far.
fun IrClass.isOptionalAnnotationClass(): Boolean =
    isAnnotationClass &&
            hasAnnotation(ExpectedActualDeclarationChecker.OPTIONAL_EXPECTATION_FQ_NAME)

val IrDeclaration.isAnnotatedWithDeprecated: Boolean
    get() = annotations.hasAnnotation(FqNames.deprecated)

val IrDeclaration.isDeprecatedCallable: Boolean
    get() = isAnnotatedWithDeprecated || origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE_FOR_COMPATIBILITY

// We can't check for JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS because for interface methods
// moved to DefaultImpls, origin is changed to DEFAULT_IMPLS
// TODO: Fix origin somehow
val IrFunction.isSyntheticMethodForProperty: Boolean
    get() = name.asString().endsWith(JvmAbi.ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX)

val IrFunction.isDeprecatedFunction: Boolean
    get() = isSyntheticMethodForProperty || isDeprecatedCallable ||
            (this as? IrSimpleFunction)?.correspondingPropertySymbol?.owner?.isDeprecatedCallable == true ||
            isAccessorForDeprecatedPropertyImplementedByDelegation

private val IrFunction.isAccessorForDeprecatedPropertyImplementedByDelegation: Boolean
    get() =
        origin == IrDeclarationOrigin.DELEGATED_MEMBER &&
                this is IrSimpleFunction &&
                correspondingPropertySymbol != null &&
                overriddenSymbols.any {
                    it.owner.correspondingPropertySymbol?.owner?.isDeprecatedCallable == true
                }

@OptIn(ObsoleteDescriptorBasedAPI::class)
val IrDeclaration.psiElement: PsiElement?
    get() = (descriptor as? DeclarationDescriptorWithSource)?.psiElement

@OptIn(ObsoleteDescriptorBasedAPI::class)
val IrMemberAccessExpression<*>.psiElement: PsiElement?
    get() = (symbol.descriptor.original as? DeclarationDescriptorWithSource)?.psiElement

fun IrSimpleType.isRawType(): Boolean =
    hasAnnotation(JvmGeneratorExtensions.RAW_TYPE_ANNOTATION_FQ_NAME)

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun classFileContainsMethod(function: IrFunction, context: JvmBackendContext, name: String): Boolean? {
    val originalDescriptor = context.methodSignatureMapper.mapSignatureWithGeneric(function).asmMethod.descriptor
    val descriptor = if (function.isSuspend)
        listOf(*Type.getArgumentTypes(originalDescriptor), Type.getObjectType("kotlin/coroutines/Continuation"))
            .joinToString(prefix = "(", postfix = ")", separator = "") + AsmTypes.OBJECT_TYPE
    else originalDescriptor
    return classFileContainsMethod(function.descriptor, context.state, Method(name, descriptor))
}
