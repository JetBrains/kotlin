/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.codegen.ASSERTIONS_DISABLED_FIELD_NAME
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.codegen.mangleNameIfNeeded
import org.jetbrains.kotlin.codegen.state.JvmBackendConfig
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.deserialization.PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClass
import org.jetbrains.kotlin.ir.declarations.lazy.IrMaybeDeserializedClass
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_DEFAULT_FQ_NAME
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_DEFAULT_WITH_COMPATIBILITY_FQ_NAME
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.multiplatform.OptionalAnnotationUtil
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method
import java.io.File

fun IrDeclaration.getJvmNameFromAnnotation(): String? {
    // TODO lower @JvmName?
    val const = getAnnotation(DescriptorUtils.JVM_NAME)?.getValueArgument(0) as? IrConst<*> ?: return null
    val value = const.value as? String ?: return null
    return when (origin) {
        IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER -> "$value\$default"
        JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE,
        JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE -> "$value$FOR_INLINE_SUFFIX"
        else -> value
    }
}

val IrFunction.propertyIfAccessor: IrDeclaration
    get() = (this as? IrSimpleFunction)?.correspondingPropertySymbol?.owner ?: this

fun IrFunction.isSimpleFunctionCompiledToJvmDefault(jvmDefaultMode: JvmDefaultMode): Boolean {
    return (this as? IrSimpleFunction)?.isCompiledToJvmDefault(jvmDefaultMode) == true
}

fun IrSimpleFunction.isCompiledToJvmDefault(jvmDefaultMode: JvmDefaultMode): Boolean {
    assert(!isFakeOverride && parentAsClass.isInterface && modality != Modality.ABSTRACT) {
        "`isCompiledToJvmDefault` should be called on non-fakeoverrides and non-abstract methods from interfaces ${ir2string(this)}"
    }
    if (origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) return false
    if (hasJvmDefault()) return true
    when (val klass = parentAsClass) {
        is IrLazyClass -> klass.classProto?.let {
            return JvmProtoBufUtil.isNewPlaceForBodyGeneration(it)
        }
        is IrMaybeDeserializedClass -> return klass.isNewPlaceForBodyGeneration
    }
    return jvmDefaultMode.isEnabled
}

fun IrFunction.hasJvmDefault(): Boolean = propertyIfAccessor.hasAnnotation(JVM_DEFAULT_FQ_NAME)
fun IrClass.hasJvmDefaultNoCompatibilityAnnotation(): Boolean = hasAnnotation(JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME)
fun IrClass.hasJvmDefaultWithCompatibilityAnnotation(): Boolean = hasAnnotation(JVM_DEFAULT_WITH_COMPATIBILITY_FQ_NAME)
fun IrFunction.hasPlatformDependent(): Boolean = propertyIfAccessor.hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME)

fun IrFunction.getJvmVisibilityOfDefaultArgumentStub() =
    if (DescriptorVisibilities.isPrivate(visibility) || isInlineOnly()) JavaDescriptorVisibilities.PACKAGE_VISIBILITY else DescriptorVisibilities.PUBLIC

fun IrDeclaration.isInCurrentModule(): Boolean =
    getPackageFragment() is IrFile

// Determine if the IrExpression is smartcast, and if so, if it is cast from higher than nullable target types.
// This is needed to pinpoint exceptional treatment of IEEE754 floating point comparisons, where proper IEEE
// comparisons are used "if values are statically known to be of primitive numeric types", taken to mean as
// "not learned through smartcasting".
fun IrExpression.isSmartcastFromHigherThanNullable(context: JvmBackendContext): Boolean {
    return when (this) {
        is IrTypeOperatorCall ->
            operator == IrTypeOperator.IMPLICIT_CAST && !argument.type.isSubtypeOf(type.makeNullable(), context.typeSystem)
        is IrGetValue -> {
            // Check if the variable initializer is smartcast. In FIR, if the subject of a `when` is smartcast,
            // the IMPLICIT_CAST is in the initializer of the variable for the subject.
            val variable = (symbol as? IrVariableSymbol)?.owner ?: return false
            !variable.isVar && variable.initializer?.isSmartcastFromHigherThanNullable(context) == true
        }
        else -> false
    }
}

fun IrElement.replaceThisByStaticReference(
    cachedFields: CachedFieldsForObjectInstances,
    irClass: IrClass,
    oldThisReceiverParameter: IrValueParameter
) {
    transformChildrenVoid(object : IrElementTransformerVoid() {
        override fun visitGetValue(expression: IrGetValue): IrExpression =
            if (expression.symbol == oldThisReceiverParameter.symbol) {
                IrGetFieldImpl(
                    expression.startOffset,
                    expression.endOffset,
                    cachedFields.getPrivateFieldForObjectInstance(irClass).symbol,
                    irClass.defaultType
                )
            } else super.visitGetValue(expression)
    })
}

// TODO: Interface Parameters
//
// The call sites using this function share that they are calling an
// interface method that has been moved to a DefaultImpls class. In that
// process, the type parameters of the interface are introduced as the first
// parameters to the method. When rewriting calls to point to the new method,
// the instantiation `S,T` of the interface type `I<S,T>` for the _calling_
// class `C` gives the proper instantiation fo arguments.
//
// We essentially want to answer the type query:
//
// C <: I<?S,?T>
//
// And put that instantiation as the first type parameters to the call, filling
// in whatever type arguments are provided at call the call site for the rest.
// The front-end type checking guarantees this is well-formed.
//
// For now, we put `Any?`.
fun createPlaceholderAnyNType(irBuiltIns: IrBuiltIns): IrType =
    irBuiltIns.anyNType

fun createDelegatingCallWithPlaceholderTypeArguments(
    existingCall: IrCall,
    redirectTarget: IrSimpleFunction,
    irBuiltIns: IrBuiltIns
): IrCall =
    IrCallImpl(
        existingCall.startOffset,
        existingCall.endOffset,
        existingCall.type,
        redirectTarget.symbol,
        typeArgumentsCount = redirectTarget.typeParameters.size,
        valueArgumentsCount = redirectTarget.valueParameters.size,
        origin = existingCall.origin
    ).apply {
        copyFromWithPlaceholderTypeArguments(existingCall, irBuiltIns)
    }

fun IrMemberAccessExpression<IrFunctionSymbol>.copyFromWithPlaceholderTypeArguments(
    existingCall: IrMemberAccessExpression<IrFunctionSymbol>, irBuiltIns: IrBuiltIns
) {
    copyValueArgumentsFrom(existingCall, this.symbol.owner, receiversAsArguments = true, argumentsAsReceivers = false)
    var offset = 0
    existingCall.symbol.owner.parentAsClass.typeParameters.forEach { _ ->
        putTypeArgument(offset++, createPlaceholderAnyNType(irBuiltIns))
    }
    for (i in 0 until existingCall.typeArgumentsCount) {
        putTypeArgument(i + offset, existingCall.getTypeArgument(i))
    }
}

// Check whether a function maps to an abstract method.
// For non-interface methods or interface methods coming from Java the modality is correct. Kotlin interface methods
// are abstract unless they are annotated @PlatformDependent or compiled to JVM default (with @JvmDefault annotation or without)
// or they override such method.
fun IrSimpleFunction.isJvmAbstract(jvmDefaultMode: JvmDefaultMode): Boolean {
    if (modality == Modality.ABSTRACT) return true
    if (!parentAsClass.isJvmInterface) return false
    return resolveFakeOverride()?.run { !isCompiledToJvmDefault(jvmDefaultMode) && !hasPlatformDependent() } != false
}

fun firstSuperMethodFromKotlin(
    override: IrSimpleFunction,
    implementation: IrSimpleFunction
): IrSimpleFunctionSymbol {
    return override.overriddenSymbols.firstOrNull {
        val owner = it.owner
        owner.modality != Modality.ABSTRACT && owner.overrides(implementation)
    } ?: error("No super method found for: ${override.render()}")
}

// MethodSignatureMapper uses the corresponding property of a function to determine correct names
// for property accessors.
fun IrSimpleFunction.copyCorrespondingPropertyFrom(source: IrSimpleFunction) {
    val property = source.correspondingPropertySymbol?.owner ?: return
    val target = this

    correspondingPropertySymbol = factory.buildProperty {
        name = property.name
        updateFrom(property)
    }.apply {
        parent = target.parent
        annotations = property.annotations
        when {
            source.isGetter -> getter = target
            source.isSetter -> setter = target
            else -> error("Orphaned property getter/setter: ${source.render()}")
        }
    }.symbol
}

fun IrProperty.needsAccessor(accessor: IrSimpleFunction): Boolean = when {
    // Properties in annotation classes become abstract methods named after the property.
    (parent as? IrClass)?.kind == ClassKind.ANNOTATION_CLASS -> true
    // Multi-field value class accessors must always be added.
    accessor.isGetter && accessor.contextReceiverParametersCount == 0 && accessor.extensionReceiverParameter == null &&
            accessor.returnType.needsMfvcFlattening() -> true
    accessor.isSetter && accessor.contextReceiverParametersCount == 0 && accessor.extensionReceiverParameter == null &&
            accessor.valueParameters.single().type.needsMfvcFlattening() -> true
    // @JvmField properties have no getters/setters
    resolveFakeOverride()?.backingField?.hasAnnotation(JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME) == true -> false
    // We do not produce default accessors for private fields
    else -> accessor.origin != IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR || !DescriptorVisibilities.isPrivate(accessor.visibility)
}

val IrDeclaration.isStaticInlineClassReplacement: Boolean
    get() = origin == JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT
            || origin == JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_CONSTRUCTOR

val IrDeclaration.isStaticMultiFieldValueClassReplacement: Boolean
    get() = origin == JvmLoweredDeclarationOrigin.STATIC_MULTI_FIELD_VALUE_CLASS_REPLACEMENT
            || origin == JvmLoweredDeclarationOrigin.STATIC_MULTI_FIELD_VALUE_CLASS_CONSTRUCTOR

val IrDeclaration.isStaticValueClassReplacement: Boolean
    get() = isStaticMultiFieldValueClassReplacement || isStaticInlineClassReplacement

// On the IR backend we represent raw types as star projected types with a special synthetic annotation.
// See `TypeTranslator.translateTypeAnnotations`.
private fun JvmBackendContext.makeRawTypeAnnotation() =
    IrConstructorCallImpl.fromSymbolOwner(
        generatorExtensions.rawTypeAnnotationConstructor!!.constructedClassType,
        generatorExtensions.rawTypeAnnotationConstructor!!.symbol
    )

fun IrClass.rawType(context: JvmBackendContext): IrType =
    defaultType.addAnnotations(listOf(context.makeRawTypeAnnotation()))

fun IrSimpleType.isRawType(): Boolean =
    hasAnnotation(JvmSymbols.RAW_TYPE_ANNOTATION_FQ_NAME)

val IrClass.isJvmInterface: Boolean
    get() = isAnnotationClass || isInterface

fun IrClass.getSingleAbstractMethod(): IrSimpleFunction? =
    functions.singleOrNull { it.modality == Modality.ABSTRACT }

fun IrFile.getKtFile(): KtFile? =
    (fileEntry as? PsiIrFileEntry)?.psiFile as KtFile?

fun IrFile.getIoFile(): File? =
    when (val fe = fileEntry) {
        is PsiIrFileEntry -> fe.psiFile.virtualFile?.path?.let(::File)
        else -> File(fe.name)
    }

inline fun IrElement.hasChild(crossinline block: (IrElement) -> Boolean): Boolean {
    var result = false
    acceptChildren(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) = when {
            result -> Unit
            block(element) -> result = true
            else -> element.acceptChildren(this, null)
        }
    }, null)
    return result
}

val IrClass.isSyntheticSingleton: Boolean
    get() = (origin == JvmLoweredDeclarationOrigin.LAMBDA_IMPL
            || origin == JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
            || origin == JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE)
            && primaryConstructor!!.valueParameters.isEmpty()

fun IrSimpleFunction.suspendFunctionOriginal(): IrSimpleFunction =
    if (isSuspend &&
        !isStaticValueClassReplacement &&
        !isOrOverridesDefaultParameterStub() &&
        parentClassOrNull?.origin != JvmLoweredDeclarationOrigin.DEFAULT_IMPLS
    )
        attributeOwnerId as IrSimpleFunction
    else this

fun IrFunction.suspendFunctionOriginal(): IrFunction =
    (this as? IrSimpleFunction)?.suspendFunctionOriginal() ?: this

private fun IrSimpleFunction.isOrOverridesDefaultParameterStub(): Boolean =
    // Cannot use resolveFakeOverride here because of KT-36188.
    DFS.ifAny(
        listOf(this),
        { it.overriddenSymbols.map(IrSimpleFunctionSymbol::owner) },
        { it.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER }
    )

fun IrClass.buildAssertionsDisabledField(backendContext: JvmBackendContext, topLevelClass: IrClass) =
    factory.buildField {
        name = Name.identifier(ASSERTIONS_DISABLED_FIELD_NAME)
        origin = JvmLoweredDeclarationOrigin.GENERATED_ASSERTION_ENABLED_FIELD
        visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY
        type = backendContext.irBuiltIns.booleanType
        isFinal = true
        isStatic = true
    }.also { field ->
        field.parent = this
        field.initializer = backendContext.createJvmIrBuilder(this.symbol).run {
            at(field)
            irExprBody(irNot(irCall(irSymbols.desiredAssertionStatus).apply {
                dispatchReceiver = javaClassReference(topLevelClass.defaultType)
            }))
        }
    }

fun IrField.isAssertionsDisabledField(context: JvmBackendContext) =
    name.asString() == ASSERTIONS_DISABLED_FIELD_NAME && type == context.irBuiltIns.booleanType && isStatic

fun IrClass.hasAssertionsDisabledField(context: JvmBackendContext) =
    fields.any { it.isAssertionsDisabledField(context) }

fun IrField.constantValue(): IrConst<*>? {
    val value = initializer?.expression as? IrConst<*> ?: return null

    // JVM has a ConstantValue attribute which does two things:
    //   1. allows the field to be inlined into other modules;
    //   2. implicitly generates an initialization of that field in <clinit>
    // It is only allowed on final fields of primitive/string types. Java applies it whenever possible; Kotlin only applies it to
    // `const val`s to avoid making values part of the library's ABI unless explicitly requested by the author.
    val implicitConst = isFinal && isStatic && origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
    return if (implicitConst || correspondingPropertySymbol?.owner?.isConst == true) value else null
}

fun IrBuilderWithScope.kClassReference(classType: IrType): IrClassReference =
    IrClassReferenceImpl(
        startOffset, endOffset, context.irBuiltIns.kClassClass.starProjectedType, context.irBuiltIns.kClassClass, classType
    )

fun JvmIrBuilder.kClassToJavaClass(kClassReference: IrExpression): IrCall =
    irGet(irSymbols.javaLangClass.starProjectedType, null, irSymbols.kClassJavaPropertyGetter.symbol).apply {
        extensionReceiver = kClassReference
    }

fun JvmIrBuilder.javaClassReference(classType: IrType): IrCall =
    kClassToJavaClass(kClassReference(classType))

fun IrDeclarationParent.getCallableReferenceOwnerKClassType(context: JvmBackendContext): IrType =
    if (this is IrClass) defaultType
    else {
        // For built-in members (i.e. top level `toString`) we generate reference to an internal class for an owner.
        // This allows kotlin-reflect to understand that this is a built-in intrinsic which has no real declaration,
        // and construct a special KCallable object.
        context.ir.symbols.intrinsicsKotlinClass.defaultType
    }

fun IrDeclaration.getCallableReferenceTopLevelFlag(): Int =
    if (parent.let { it is IrClass && it.isFileClass }) 1 else 0

// Based on KotlinTypeMapper.findSuperDeclaration.
fun findSuperDeclaration(function: IrSimpleFunction, isSuperCall: Boolean, jvmDefaultMode: JvmDefaultMode): IrSimpleFunction {
    var current = function
    while (current.isFakeOverride) {
        // TODO: probably isJvmInterface instead of isInterface, here and in KotlinTypeMapper
        val classCallable = current.overriddenSymbols.firstOrNull { !it.owner.parentAsClass.isInterface }?.owner
        if (classCallable != null) {
            current = classCallable
            continue
        }
        if (isSuperCall && !current.parentAsClass.isInterface) {
            val overridden = current.resolveFakeOverride()
            if (overridden != null && (overridden.isMethodOfAny() || !overridden.isCompiledToJvmDefault(jvmDefaultMode))) {
                return current
            }
        }

        current = current.overriddenSymbols.firstOrNull()?.owner
            ?: error("Fake override should have at least one overridden descriptor: ${current.render()}")
    }
    return current
}

fun IrMemberAccessExpression<*>.getIntConstArgumentOrNull(i: Int) = getValueArgument(i)?.let {
    if (it is IrConst<*> && it.kind == IrConstKind.Int)
        it.value as Int
    else
        null
}

fun IrMemberAccessExpression<*>.getIntConstArgument(i: Int): Int =
    getIntConstArgumentOrNull(i) ?: throw AssertionError("Value argument #$i should be an Int const: ${dump()}")

fun IrMemberAccessExpression<*>.getStringConstArgument(i: Int): String =
    getValueArgument(i)?.let {
        if (it is IrConst<*> && it.kind == IrConstKind.String)
            it.value as String
        else
            null
    } ?: throw AssertionError("Value argument #$i should be a String const: ${dump()}")

fun IrMemberAccessExpression<*>.getBooleanConstArgument(i: Int): Boolean =
    getValueArgument(i)?.let {
        if (it is IrConst<*> && it.kind == IrConstKind.Boolean)
            it.value as Boolean
        else
            null
    } ?: throw AssertionError("Value argument #$i should be a Boolean const: ${dump()}")

val IrDeclaration.fileParent: IrFile
    get() = fileParentOrNull ?: error("No file parent: $this")

@Suppress("RecursivePropertyAccessor")
val IrDeclaration.fileParentOrNull: IrFile?
    get() = when (val myParent = parent) {
        is IrFile -> myParent
        is IrDeclaration -> myParent.fileParentOrNull
        else -> null
    }

val IrMemberWithContainerSource.parentClassId: ClassId?
    get() {
        val directMember = (this as? IrSimpleFunction)?.correspondingPropertySymbol?.owner ?: this

        return when (val containerSource = directMember.containerSource) {
            is JvmPackagePartSource -> containerSource.classId
            is FacadeClassSource -> ClassId.topLevel(containerSource.className.fqNameForClassNameWithoutDollars)
            else -> (directMember.parent as? IrClass)?.classId
        }
    }

// Translated into IR-based terms from classifierDescriptor?.classId
private val IrClass.classId: ClassId?
    get() = when (val parent = parent) {
        is IrExternalPackageFragment -> ClassId(parent.packageFqName, name)
        // TODO: there's `context.classNameOverride`; theoretically it's only relevant for top-level members,
        //       where `containerSource` is a `JvmPackagePartSource` anyway, but I'm not 100% sure.
        is IrClass -> parent.classId?.createNestedClassId(name)
        else -> null
    }

val IrClass.isOptionalAnnotationClass: Boolean
    get() = kind == ClassKind.ANNOTATION_CLASS &&
            isExpect &&
            hasAnnotation(OptionalAnnotationUtil.OPTIONAL_EXPECTATION_FQ_NAME)

fun IrFunctionAccessExpression.receiverAndArgs(): List<IrExpression> {
    return (arrayListOf(this.dispatchReceiver, this.extensionReceiver) +
            symbol.owner.valueParameters.mapIndexed { i, _ -> getValueArgument(i) }).filterNotNull()
}

fun classFileContainsMethod(classId: ClassId, function: IrFunction, context: JvmBackendContext): Boolean? {
    val originalSignature = context.defaultMethodSignatureMapper.mapAsmMethod(function)
    val originalDescriptor = originalSignature.descriptor
    val descriptor = if (function.isSuspend)
        listOf(*Type.getArgumentTypes(originalDescriptor), Type.getObjectType("kotlin/coroutines/Continuation"))
            .joinToString(prefix = "(", postfix = ")", separator = "") + AsmTypes.OBJECT_TYPE
    else originalDescriptor
    return org.jetbrains.kotlin.codegen.classFileContainsMethod(classId, context.state, Method(originalSignature.name, descriptor))
}

val DeclarationDescriptorWithSource.psiElement: PsiElement?
    get() = (source as? PsiSourceElement)?.psi

@OptIn(ObsoleteDescriptorBasedAPI::class)
val IrDeclaration.psiElement: PsiElement?
    get() = (descriptor as? DeclarationDescriptorWithSource)?.psiElement

@OptIn(ObsoleteDescriptorBasedAPI::class)
val IrMemberAccessExpression<*>.psiElement: PsiElement?
    get() = (symbol.descriptor.original as? DeclarationDescriptorWithSource)?.psiElement

fun IrFunction.extensionReceiverName(config: JvmBackendConfig): String {
    if (!config.languageVersionSettings.supportsFeature(LanguageFeature.NewCapturedReceiverFieldNamingConvention)) {
        return AsmUtil.RECEIVER_PARAMETER_NAME
    }

    extensionReceiverParameter?.let {
        if (it.name.asString().startsWith(AsmUtil.LABELED_THIS_PARAMETER)) {
            return it.name.asString()
        }
    }
    val callableName = (this as? IrSimpleFunction)?.correspondingPropertySymbol?.owner?.name ?: name
    return if (callableName.isSpecial || !Name.isValidIdentifier(callableName.asString()))
        AsmUtil.RECEIVER_PARAMETER_NAME
    else
        AsmUtil.LABELED_THIS_PARAMETER + mangleNameIfNeeded(callableName.asString())
}

fun IrFunction.isBridge(): Boolean =
    origin == IrDeclarationOrigin.BRIDGE || origin == IrDeclarationOrigin.BRIDGE_SPECIAL

// Enum requires external implementation of entries if it's either a Java enum, or a Kotlin enum compiled with pre-1.8 LV/AV.
fun IrClass.isEnumClassWhichRequiresExternalEntries(): Boolean =
    isEnumClass && (isFromJava() || !hasEnumEntriesFunction())

private fun IrClass.hasEnumEntriesFunction(): Boolean {
    // Enums from the current module will have a property `entries` if they are unlowered yet (i.e. enum is declared in another file
    // which will be lowered after the file with the call site), or a function `<get-entries>` if they are already lowered.
    // Enums from other modules have `entries` if and only if the flag `hasEnumEntries` is true.
    return if (isInCurrentModule())
        functions.any { it.isGetEntries() } || properties.any { it.getter?.isGetEntries() == true }
    else hasEnumEntries
}

private fun IrSimpleFunction.isGetEntries(): Boolean =
    name.toString() == "<get-entries>"
            && dispatchReceiverParameter == null
            && extensionReceiverParameter == null
            && valueParameters.isEmpty()

fun IrClass.findEnumValuesFunction(context: JvmBackendContext): IrSimpleFunction = functions.single {
    it.name.toString() == "values"
            && it.dispatchReceiverParameter == null
            && it.extensionReceiverParameter == null
            && it.valueParameters.isEmpty()
            && it.returnType.isBoxedArray
            && it.returnType.getArrayElementType(context.irBuiltIns).classOrNull == this.symbol
}
