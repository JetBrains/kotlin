/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.common.lower.allOverridden
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil.LABELED_THIS_PARAMETER
import org.jetbrains.kotlin.codegen.AsmUtil.RECEIVER_PARAMETER_NAME
import org.jetbrains.kotlin.codegen.inline.DefaultSourceMapper
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.inline.INLINE_ONLY_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmClassSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type

class IrFrameMap : FrameMapBase<IrSymbol>() {
    private val typeMap = mutableMapOf<IrSymbol,Type>()

    override fun enter(descriptor: IrSymbol, type: Type): Int {
        typeMap[descriptor] = type
        return super.enter(descriptor, type)
    }

    override fun leave(descriptor: IrSymbol): Int {
        typeMap.remove(descriptor)
        return super.leave(descriptor)
    }

    fun typeOf(descriptor: IrSymbol): Type = typeMap.getValue(descriptor)
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

internal val IrDeclaration.fileParent: IrFile
    get() {
        val myParent = parent
        return when (myParent) {
            is IrFile -> myParent
            else -> (myParent as IrDeclaration).fileParent
        }
    }

internal val DeclarationDescriptorWithSource.psiElement: PsiElement?
    get() = (source as? PsiSourceElement)?.psi

fun JvmBackendContext.getSourceMapper(declaration: IrClass): DefaultSourceMapper {
    val sourceManager = this.psiSourceManager
    val fileEntry = sourceManager.getFileEntry(declaration.fileParent)
    check(fileEntry != null) { "No PSI file entry found for class: ${declaration.dump()}" }
    // NOTE: apparently inliner requires the source range to cover the
    //       whole file the class is declared in rather than the class only.
    // TODO: revise
    val endLineNumber = fileEntry.getSourceRangeInfo(0, fileEntry.maxOffset).endLineNumber
    return DefaultSourceMapper(
        SourceInfo.createInfoForIr(
            endLineNumber + 1,
            this.state.typeMapper.mapType(declaration.descriptor).internalName,
            declaration.fileParent.name
        )
    )
}

val IrType.isExtensionFunctionType: Boolean
    get() = isFunctionTypeOrSubtype() && hasAnnotation(KotlinBuiltIns.FQ_NAMES.extensionFunctionType)


/* Borrowed from MemberCodegen.java */

fun writeInnerClass(innerClass: IrClass, typeMapper: IrTypeMapper, context: JvmBackendContext, v: ClassBuilder) {
    val outerClassInternalName = innerClass.parent.safeAs<IrClass>()?.let { typeMapper.classInternalName(it) }
    /* TODO: So long as the type mapper works by descriptors, the internal name should be consistent with it. */
    val outerClassInternalNameByDescriptor = innerClass.descriptor.containingDeclaration.safeAs<ClassDescriptor>()?.let {
        typeMapper.kotlinTypeMapper.classInternalName(it)
    }
    val innerName = innerClass.name.takeUnless { it.isSpecial }?.asString()
    val innerClassInternalName = typeMapper.classInternalName(innerClass)
    v.visitInnerClass(innerClassInternalName, outerClassInternalNameByDescriptor, innerName, innerClass.calculateInnerClassAccessFlags(context))
}

/* Borrowed with modifications from AsmUtil.java */

private val NO_FLAG_LOCAL = 0

private val visibilityToAccessFlag = mapOf(
    Visibilities.PRIVATE to Opcodes.ACC_PRIVATE,
    Visibilities.PRIVATE_TO_THIS to Opcodes.ACC_PRIVATE,
    Visibilities.PROTECTED to Opcodes.ACC_PROTECTED,
    JavaVisibilities.PROTECTED_STATIC_VISIBILITY to Opcodes.ACC_PROTECTED,
    JavaVisibilities.PROTECTED_AND_PACKAGE to Opcodes.ACC_PROTECTED,
    Visibilities.PUBLIC to Opcodes.ACC_PUBLIC,
    Visibilities.INTERNAL to Opcodes.ACC_PUBLIC,
    Visibilities.LOCAL to NO_FLAG_LOCAL,
    JavaVisibilities.PACKAGE_VISIBILITY to AsmUtil.NO_FLAG_PACKAGE_PRIVATE
)

private fun IrDeclaration.getVisibilityAccessFlagForAnonymous(): Int =
    if (isInlineOrContainedInInline(parent as? IrDeclaration)) Opcodes.ACC_PUBLIC else AsmUtil.NO_FLAG_PACKAGE_PRIVATE

fun IrClass.calculateInnerClassAccessFlags(context: JvmBackendContext): Int {
    val isLambda = superTypes.any { it.safeAs<IrSimpleType>()?.classifier === context.ir.symbols.lambdaClass }
    val visibility = when {
        isLambda -> getVisibilityAccessFlagForAnonymous()
        visibility === Visibilities.LOCAL -> Opcodes.ACC_PUBLIC
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

private fun IrDeclarationWithVisibility.getVisibilityAccessFlag(kind: OwnerKind? = null): Int =
    specialCaseVisibility(kind)
        ?: visibilityToAccessFlag[visibility]
        ?: throw IllegalStateException("$visibility is not a valid visibility in backend for ${ir2string(this)}")


private fun IrDeclarationWithVisibility.specialCaseVisibility(kind: OwnerKind?): Int? {
//    if (JvmCodegenUtil.isNonIntrinsicPrivateCompanionObjectInInterface(memberDescriptor)) {
//        return ACC_PUBLIC
//    }
    if (this is IrClass && Visibilities.isPrivate(visibility) &&
            parent.safeAs<IrClass>()?.isInterface ?: false) { // TODO: non-intrinsic
        return Opcodes.ACC_PUBLIC
    }

//    if (memberDescriptor is FunctionDescriptor && isInlineClassWrapperConstructor(memberDescriptor, kind))
    if (this is IrConstructor && parentAsClass.isInline && kind === OwnerKind.IMPLEMENTATION) {
        return Opcodes.ACC_PRIVATE
    }

//    if (kind !== OwnerKind.ERASED_INLINE_CLASS &&
//        memberDescriptor is ConstructorDescriptor &&
//        memberDescriptor !is AccessorForConstructorDescriptor &&
//        shouldHideConstructorDueToInlineClassTypeValueParameters(memberDescriptor)
//    ) {
    if (kind !== OwnerKind.ERASED_INLINE_CLASS &&
        this is IrConstructor &&
        shouldHideDueToInlineClassTypeValueParameters()
    ) {
        return Opcodes.ACC_PRIVATE
    }

//    if (memberDescriptor.isEffectivelyInlineOnly()) {
    if (isEffectivelyInlineOnly()) {
        return Opcodes.ACC_PRIVATE
    }

//    if (memberVisibility === Visibilities.LOCAL && memberDescriptor is CallableMemberDescriptor) {
    if (visibility === Visibilities.LOCAL && this is IrFunction) {
        return Opcodes.ACC_PUBLIC
    }

//    if (isEnumEntry(memberDescriptor)) {
    if (this is IrClass && this.kind === ClassKind.ENUM_ENTRY) {
        return AsmUtil.NO_FLAG_PACKAGE_PRIVATE
    }

//    These ones should be public anyway after ToArrayLowering.
//    if (memberDescriptor.isToArrayFromCollection()) {
//        return ACC_PUBLIC
//    }

//    if (memberDescriptor is ConstructorDescriptor && isAnonymousObject(memberDescriptor.containingDeclaration)) {
//        return getVisibilityAccessFlagForAnonymous(memberDescriptor.containingDeclaration as ClassDescriptor)
//    }
    if (this is IrConstructor && parentAsClass.isAnonymousObject) {
        return parentAsClass.getVisibilityAccessFlagForAnonymous()
    }

//    TODO: when is this applicable?
//    if (memberDescriptor is SyntheticJavaPropertyDescriptor) {
//        return getVisibilityAccessFlag((memberDescriptor as SyntheticJavaPropertyDescriptor).getMethod)
//    }


//    if (memberDescriptor is PropertyAccessorDescriptor) {
//        val property = memberDescriptor.correspondingProperty
//        if (property is SyntheticJavaPropertyDescriptor) {
//            val method = (if (memberDescriptor === property.getGetter())
//                (property as SyntheticJavaPropertyDescriptor).getMethod
//            else
//                (property as SyntheticJavaPropertyDescriptor).setMethod)
//                ?: error("No get/set method in SyntheticJavaPropertyDescriptor: $property")
//            return getVisibilityAccessFlag(method)
//        }
//    }
    if (this is IrField && correspondingPropertySymbol?.owner?.isExternal == true) {
        val method = correspondingPropertySymbol?.owner?.getter ?: correspondingPropertySymbol?.owner?.setter
        ?: error("No get/set method in SyntheticJavaPropertyDescriptor: ${ir2string(correspondingPropertySymbol?.owner)}")
        return method.getVisibilityAccessFlag()
    }

//    if (memberDescriptor is CallableDescriptor && memberVisibility === Visibilities.PROTECTED) {
//        for (overridden in DescriptorUtils.getAllOverriddenDescriptors(memberDescriptor as CallableDescriptor)) {
//            if (isJvmInterface(overridden.containingDeclaration)) {
//                return ACC_PUBLIC
//            }
//        }
//    }
    if (this is IrSimpleFunction && visibility === Visibilities.PROTECTED &&
        allOverridden().any { it.parentAsClass.isJvmInterface }
    ) {
        return Opcodes.ACC_PUBLIC
    }

    if (!Visibilities.isPrivate(visibility)) {
        return null
    }

    if (this is IrSimpleFunction && isSuspend) {
        return AsmUtil.NO_FLAG_PACKAGE_PRIVATE
    }

//  Should be taken care of in IR
//    if (memberDescriptor is AccessorForCompanionObjectInstanceFieldDescriptor) {
//        return NO_FLAG_PACKAGE_PRIVATE
//    }

//    return if (memberDescriptor is ConstructorDescriptor && isEnumEntry(containingDeclaration)) {
//        NO_FLAG_PACKAGE_PRIVATE
//    } else null
    if (this is IrConstructor && parentAsClass.kind === ClassKind.ENUM_ENTRY) {
        return AsmUtil.NO_FLAG_PACKAGE_PRIVATE
    }

    return null
}

/* From inlineClassManglingRules.kt */
fun IrConstructor.shouldHideDueToInlineClassTypeValueParameters() =
    !Visibilities.isPrivate(visibility) &&
            !parentAsClass.isInline &&
            parentAsClass.modality !== Modality.SEALED &&
            valueParameters.any { it.type.requiresFunctionNameMangling() }

fun IrType.requiresFunctionNameMangling(): Boolean =
    isInlineClassThatRequiresMangling() || isTypeParameterWithUpperBoundThatRequiresMangling()

fun IrType.isInlineClassThatRequiresMangling() =
    safeAs<IrSimpleType>()?.classifier?.owner?.safeAs<IrClass>()?.let {
        it.isInline && !it.isDontMangleClass()
    } ?: false

fun IrClass.isDontMangleClass() =
    fqNameWhenAvailable != DescriptorUtils.RESULT_FQ_NAME

fun IrType.isTypeParameterWithUpperBoundThatRequiresMangling() =
    safeAs<IrSimpleType>()?.classifier?.owner.safeAs<IrTypeParameter>()?.let { param ->
        param.superTypes.any { it.requiresFunctionNameMangling() }
    } ?: false


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
    isInlineOnlyOrReifiable() ||
            (this is IrSimpleFunction && isSuspend && isInline &&
                    (valueParameters.any { it.isCrossinline } || visibility === Visibilities.PRIVATE))

private fun IrFunction.isInlineOnly() =
    isInline && hasAnnotation(INLINE_ONLY_ANNOTATION_FQ_NAME)

private fun IrFunction.isReifiable() = typeParameters.any { it.isReified }


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

internal class IrSuperClassInfo(val type: Type, val irType: IrType?)

internal fun getSignature(
    irClass: IrClass,
    classAsmType: Type,
    superClassInfo: IrSuperClassInfo,
    typeMapper: IrTypeMapper
): JvmClassSignature {
    val sw = BothSignatureWriter(BothSignatureWriter.Mode.CLASS)

    typeMapper.writeFormalTypeParameters(irClass.typeParameters, sw)

    sw.writeSuperclass()
    val irType = superClassInfo.irType
    if (irType == null) {
        sw.writeClassBegin(superClassInfo.type)
        sw.writeClassEnd()
    } else {
        typeMapper.mapSupertype(irType, sw)
    }
    sw.writeSuperclassEnd()

    val superInterfaces = LinkedHashSet<String>()
    val kotlinMarkerInterfaces = LinkedHashSet<String>()

    for (superType in irClass.superTypes) {
        val superClass = superType.safeAs<IrSimpleType>()?.classifier?.safeAs<IrClassSymbol>()?.owner ?: continue
        if (superClass.isJvmInterface) {
            val kotlinInterfaceName = superClass.fqNameWhenAvailable!!

            sw.writeInterface()
            val jvmInterfaceType = typeMapper.mapSupertype(superType, sw)
            sw.writeInterfaceEnd()
            val jvmInterfaceInternalName = jvmInterfaceType.internalName

            superInterfaces.add(jvmInterfaceInternalName)

            val kotlinMarkerInterfaceInternalName = KOTLIN_MARKER_INTERFACES.get(kotlinInterfaceName)
            if (kotlinMarkerInterfaceInternalName != null) {
                if (typeMapper.classBuilderMode === ClassBuilderMode.LIGHT_CLASSES) {
                    sw.writeInterface()
                    val kotlinCollectionType =
                        typeMapper.mapType(superClass.defaultType, TypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS, sw)
                    sw.writeInterfaceEnd()
                    superInterfaces.add(kotlinCollectionType.internalName)
                }

                kotlinMarkerInterfaces.add(kotlinMarkerInterfaceInternalName)
            }
        }
    }

    for (kotlinMarkerInterface in kotlinMarkerInterfaces) {
        sw.writeInterface()
        sw.writeAsmType(Type.getObjectType(kotlinMarkerInterface))
        sw.writeInterfaceEnd()
    }

    superInterfaces.addAll(kotlinMarkerInterfaces)

    return JvmClassSignature(
        classAsmType.internalName, superClassInfo.type.internalName,
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
    if (isOptionalAnnotationClass()) {
        return AsmUtil.NO_FLAG_PACKAGE_PRIVATE
    }
    if (kind == ClassKind.ENUM_ENTRY) {
        return AsmUtil.NO_FLAG_PACKAGE_PRIVATE
    }
    return if (visibility === Visibilities.PUBLIC ||
        visibility === Visibilities.PROTECTED ||
        // TODO: should be package private, but for now Kotlin's reflection can't access members of such classes
        visibility === Visibilities.LOCAL ||
        visibility === Visibilities.INTERNAL
    ) {
        Opcodes.ACC_PUBLIC
    } else AsmUtil.NO_FLAG_PACKAGE_PRIVATE
}

/* Borrowed and translated from ExpectedActualDeclarationChecker */
// TODO: Descriptor-based code also checks for `descriptor.isExpect`; we don't represent expect/actual distinction in IR thus far.
fun IrClass.isOptionalAnnotationClass(): Boolean =
    isAnnotationClass &&
            hasAnnotation(ExpectedActualDeclarationChecker.OPTIONAL_EXPECTATION_FQ_NAME)

//@JvmOverloads
//fun OtherOriginForIr(element: PsiElement?, descriptor: DeclarationDescriptor? = null) =
//    if (element == null && descriptor == null)
//        JvmDeclarationOrigin.NO_ORIGIN
//    else
//        object : JvmDeclarationOrigin(JvmDeclarationOriginKind.OTHER, element, descriptor) {
//            override val element get() =
//                error("Access to PsiElement")
//            override val descriptor get() =
//                error("Access to descriptor")
//        }

//        JvmDeclarationOrigin(OTHER, element, descriptor)

/* From generateJava8ParameterNames.kt */

fun generateParameterNames(
    irFunction: IrFunction,
    mv: MethodVisitor,
    jvmSignature: JvmMethodSignature,
    state: GenerationState,
    isSynthetic: Boolean
) {
    if (!state.generateParametersMetadata || isSynthetic) {
        return
    }

    val iterator = irFunction.valueParameters.iterator()
    val kotlinParameterTypes = jvmSignature.valueParameters
    var isEnumName = true

    kotlinParameterTypes.forEachIndexed { index, parameterSignature ->
        val kind = parameterSignature.kind

        val name = when (kind) {
            JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL -> {
                isEnumName = !isEnumName
                if (!isEnumName) "\$enum\$name" else "\$enum\$ordinal"
            }
            JvmMethodParameterKind.RECEIVER -> {
                getNameForReceiverParameter(irFunction, state.languageVersionSettings)
            }
            JvmMethodParameterKind.OUTER -> AsmUtil.CAPTURED_THIS_FIELD
            JvmMethodParameterKind.VALUE -> iterator.next().name.asString()

            JvmMethodParameterKind.CONSTRUCTOR_MARKER,
            JvmMethodParameterKind.SUPER_CALL_PARAM,
            JvmMethodParameterKind.CAPTURED_LOCAL_VARIABLE,
            JvmMethodParameterKind.THIS -> {
                //we can't generate null name cause of jdk problem #9045294
                "arg" + index
            }
        }

        //A construct emitted by a Java compiler must be marked as synthetic if it does not correspond to a construct declared explicitly or
        // implicitly in source code, unless the emitted construct is a class initialization method (JVMS §2.9).
        //A construct emitted by a Java compiler must be marked as mandated if it corresponds to a formal parameter
        // declared implicitly in source code (§8.8.1, §8.8.9, §8.9.3, §15.9.5.1).
        val access = when (kind) {
            JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL -> Opcodes.ACC_SYNTHETIC
            JvmMethodParameterKind.RECEIVER -> Opcodes.ACC_MANDATED
            JvmMethodParameterKind.OUTER -> Opcodes.ACC_MANDATED
            JvmMethodParameterKind.VALUE -> 0

            JvmMethodParameterKind.CONSTRUCTOR_MARKER,
            JvmMethodParameterKind.SUPER_CALL_PARAM,
            JvmMethodParameterKind.CAPTURED_LOCAL_VARIABLE,
            JvmMethodParameterKind.THIS -> Opcodes.ACC_SYNTHETIC
        }

        mv.visitParameter(name, access)
    }
}

/* From AsmUtil.java */

fun getNameForReceiverParameter(
    irFunction: IrFunction,
    languageVersionSettings: LanguageVersionSettings
): String {
    return getLabeledThisNameForReceiver(
        irFunction, languageVersionSettings, LABELED_THIS_PARAMETER, RECEIVER_PARAMETER_NAME
    )
}

private fun getLabeledThisNameForReceiver(
    irFunction: IrFunction,
    languageVersionSettings: LanguageVersionSettings,
    prefix: String,
    defaultName: String
): String {
    if (!languageVersionSettings.supportsFeature(LanguageFeature.NewCapturedReceiverFieldNamingConvention)) {
        return defaultName
    }

    // Current codegen never touches CALL_LABEL_FOR_LAMBDA_ARGUMENT
//    if (irFunction is IrSimpleFunction) {
//        val labelName = bindingContext.get(CodegenBinding.CALL_LABEL_FOR_LAMBDA_ARGUMENT, irFunction.descriptor)
//        if (labelName != null) {
//            return getLabeledThisName(labelName, prefix, defaultName)
//        }
//    }

//    val callableName = irFunction.descriptor.safeAs<VariableAccessorDescriptor>()?.correspondingVariable?.name ?: irFunction.descriptor.name
    val callableName = irFunction.safeAs<IrSimpleFunction>()?.correspondingPropertySymbol?.owner?.name ?: irFunction.name

    return if (callableName.isSpecial) {
        defaultName
    } else getLabeledThisName(callableName.asString(), prefix, defaultName)

}

fun getLabeledThisName(callableName: String, prefix: String, defaultName: String): String {
    return if (!Name.isValidIdentifier(callableName)) {
        defaultName
    } else prefix + mangleNameIfNeeded(callableName)
}
