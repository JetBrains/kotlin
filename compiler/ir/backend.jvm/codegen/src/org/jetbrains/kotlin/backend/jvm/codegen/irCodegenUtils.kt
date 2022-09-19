/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.MultifileFacadeFileEntry
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.backend.jvm.mapping.IrTypeMapper
import org.jetbrains.kotlin.backend.jvm.mapping.mapClass
import org.jetbrains.kotlin.backend.jvm.mapping.mapSupertype
import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.FrameMapBase
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.SourceInfo
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeParametersUsages
import org.jetbrains.kotlin.codegen.inline.SourceMapper
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmClassSignature
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type

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
        ?: run {
            error("No mapping for symbol: ${symbol.owner.render()}. typeMap: ${typeMap.keys}")
        }
}

internal val IrFunction.isStatic
    get() = (this.dispatchReceiverParameter == null && this !is IrConstructor)

fun IrFrameMap.enter(irDeclaration: IrSymbolOwner, type: Type): Int {
    return enter(irDeclaration.symbol, type)
}

fun IrFrameMap.leave(irDeclaration: IrSymbolOwner): Int {
    return leave(irDeclaration.symbol)
}

fun JvmBackendContext.getSourceMapper(declaration: IrClass): SourceMapper {
    val fileEntry = declaration.fileParent.fileEntry
    // NOTE: apparently inliner requires the source range to cover the
    //       whole file the class is declared in rather than the class only.
    val endLineNumber = when (fileEntry) {
        is MultifileFacadeFileEntry -> 0
        else -> fileEntry.getSourceRangeInfo(0, fileEntry.maxOffset).endLineNumber
    }
    val sourceFileName = when (fileEntry) {
        is MultifileFacadeFileEntry -> fileEntry.partFiles.singleOrNull()?.name
        else -> declaration.fileParent.name
    }
    return SourceMapper(
        SourceInfo(
            sourceFileName,
            defaultTypeMapper.mapClass(declaration).internalName,
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
        it.classOrNull === context.ir.symbols.lambdaClass
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

    if (this is IrConstructor && parentAsClass.isSingleFieldValueClass && kind === OwnerKind.IMPLEMENTATION) {
        return Opcodes.ACC_PRIVATE
    }

    if (isInlineOnlyPrivateInBytecode()) {
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

    if (this is IrConstructor && parentAsClass.kind === ClassKind.ENUM_ENTRY) {
        return AsmUtil.NO_FLAG_PACKAGE_PRIVATE
    }

    return null
}

private tailrec fun isInlineOrContainedInInline(declaration: IrDeclaration?): Boolean = when {
    declaration === null -> false
    declaration is IrFunction && declaration.isInline -> true
    else -> isInlineOrContainedInInline(declaration.parent as? IrDeclaration)
}

private fun IrDeclarationWithVisibility.isInlineOnlyPrivateInBytecode(): Boolean =
    this is IrFunction && (isInlineOnly() || isPrivateInlineSuspend())

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

internal fun IrTypeMapper.mapClassSignature(irClass: IrClass, type: Type, generateBodies: Boolean): JvmClassSignature {
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

    val kotlinMarkerInterfaces = LinkedHashSet<String>()
    if (generateBodies && irClass.superTypes.any { it.isSuspendFunction() || it.isKSuspendFunction() }) {
        // Do not generate this class in the kapt3 mode (generateBodies=false), because kapt3 transforms supertypes correctly in the
        // "correctErrorTypes" mode only when the number of supertypes between PSI and bytecode is equal. Otherwise it tries to "correct"
        // the FunctionN type and fails, because that type doesn't need an import in the Kotlin source (kotlin.FunctionN), but needs one
        // in the Java source (kotlin.jvm.functions.FunctionN), and kapt3 doesn't perform any Kotlin->Java name lookup.
        kotlinMarkerInterfaces.add("kotlin/coroutines/jvm/internal/SuspendFunction")
    }

    val superInterfaces = LinkedHashSet<String>()
    for (superType in irClass.superTypes) {
        val superClass = superType.classOrNull?.owner ?: continue
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

val IrDeclaration.isAnnotatedWithDeprecated: Boolean
    get() = annotations.hasAnnotation(FqNames.deprecated)

internal fun IrDeclaration.isDeprecatedCallable(context: JvmBackendContext): Boolean =
    isAnnotatedWithDeprecated ||
            annotations.any { it.symbol == context.ir.symbols.javaLangDeprecatedConstructorWithDeprecatedFlag }

internal fun IrFunction.isDeprecatedFunction(context: JvmBackendContext): Boolean =
    origin == JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS ||
            isDeprecatedCallable(context) ||
            (this as? IrSimpleFunction)?.correspondingPropertySymbol?.owner?.isDeprecatedCallable(context) == true ||
            isAccessorForDeprecatedPropertyImplementedByDelegation ||
            isAccessorForDeprecatedJvmStaticProperty(context)

private val IrFunction.isAccessorForDeprecatedPropertyImplementedByDelegation: Boolean
    get() =
        origin == IrDeclarationOrigin.DELEGATED_MEMBER &&
                this is IrSimpleFunction &&
                correspondingPropertySymbol != null &&
                overriddenSymbols.any {
                    it.owner.correspondingPropertySymbol?.owner?.isAnnotatedWithDeprecated == true
                }

private fun IrFunction.isAccessorForDeprecatedJvmStaticProperty(context: JvmBackendContext): Boolean {
    if (origin != JvmLoweredDeclarationOrigin.JVM_STATIC_WRAPPER) return false
    val irExpressionBody = this.body as? IrExpressionBody
        ?: throw AssertionError("IrExpressionBody expected for JvmStatic wrapper:\n${this.dump()}")
    val irCall = irExpressionBody.expression as? IrCall
        ?: throw AssertionError("IrCall expected inside JvmStatic wrapper:\n${this.dump()}")
    val callee = irCall.symbol.owner
    val property = callee.correspondingPropertySymbol?.owner ?: return false
    return property.isDeprecatedCallable(context)
}

val IrClass.reifiedTypeParameters: ReifiedTypeParametersUsages
    get() {
        val tempReifiedTypeParametersUsages = ReifiedTypeParametersUsages()
        fun processTypeParameters(type: IrType) {
            for (supertypeArgument in (type as? IrSimpleType)?.arguments ?: emptyList()) {
                if (supertypeArgument is IrTypeProjection) {
                    val typeArgument = supertypeArgument.type
                    if (typeArgument.isReifiedTypeParameter) {
                        val typeParameter = typeArgument.classifierOrFail as IrTypeParameterSymbol
                        tempReifiedTypeParametersUsages.addUsedReifiedParameter(typeParameter.owner.name.asString())
                    } else {
                        processTypeParameters(typeArgument)
                    }
                }
            }
        }

        for (type in superTypes) {
            processTypeParameters(type)
        }

        return tempReifiedTypeParametersUsages
    }
