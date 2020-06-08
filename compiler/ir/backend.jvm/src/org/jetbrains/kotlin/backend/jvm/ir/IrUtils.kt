/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.common.lower.IrLoweringContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.JvmSymbols
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineOnly
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmDeclarationFactory
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.unboxInlineClass
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.deserialization.PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_DEFAULT_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor

/**
 * Perform as much type erasure as is significant for JVM signature generation.
 * Class types are kept as is, while type parameters are replaced with their
 * erased upper bounds, keeping the nullability information.
 *
 * For example, a type parameter `T?` where `T : Any`, `T : Comparable<T>` is
 * erased to `Any?`.
 *
 * Type arguments to the erased upper bound are replaced by `*`, since
 * recursive erasure could loop. For example, a type parameter
 * `T : Comparable<T>` is replaced by `Comparable<*>`.
 */
fun IrType.eraseTypeParameters() = when (this) {
    is IrErrorType -> this
    is IrSimpleType ->
        when (val owner = classifier.owner) {
            is IrClass -> IrSimpleTypeImpl(
                originalKotlinType,
                classifier,
                hasQuestionMark,
                arguments.map { it.eraseTypeParameters() },
                annotations
            )
            is IrTypeParameter -> {
                val upperBound = owner.erasedUpperBound
                IrSimpleTypeImpl(
                    upperBound.symbol,
                    isNullable(),
                    List(upperBound.typeParameters.size) { IrStarProjectionImpl },    // Should not affect JVM signature, but may result in an invalid type object
                    owner.annotations
                )
            }
            else -> error("Unknown IrSimpleType classifier kind: $owner")
        }
    else -> error("Unknown IrType kind: $this")
}

private fun IrTypeArgument.eraseTypeParameters(): IrTypeArgument = when (this) {
    is IrStarProjection -> this
    is IrTypeProjection -> makeTypeProjection(type.eraseTypeParameters(), variance)
    else -> error("Unknown IrTypeArgument kind: $this")
}

/**
 * Computes the erased class for this type parameter according to the java erasure rules.
 */
val IrTypeParameter.erasedUpperBound: IrClass
    get() {
        // Pick the (necessarily unique) non-interface upper bound if it exists
        for (type in superTypes) {
            val irClass = type.classOrNull?.owner ?: continue
            if (!irClass.isJvmInterface) return irClass
        }

        // Otherwise, choose either the first IrClass supertype or recurse.
        // In the first case, all supertypes are interface types and the choice was arbitrary.
        // In the second case, there is only a single supertype.
        return superTypes.first().erasedUpperBound
    }

val IrType.erasedUpperBound: IrClass
    get() = when (val classifier = classifierOrNull) {
        is IrClassSymbol -> classifier.owner
        is IrTypeParameterSymbol -> classifier.owner.erasedUpperBound
        else -> throw IllegalStateException()
    }

/**
 * Get the default null/0 value for the type.
 *
 * This handles unboxing of non-nullable inline class types to their underlying types and produces
 * a null/0 default value for the resulting type. When such unboxing takes place it ensures that
 * the value is not reboxed and reunboxed by the codegen by using the unsafeCoerceIntrinsic.
 */
fun IrType.defaultValue(startOffset: Int, endOffset: Int, context: JvmBackendContext): IrExpression {
    if (this !is IrSimpleType || hasQuestionMark || classOrNull?.owner?.isInline != true)
        return IrConstImpl.defaultValueForType(startOffset, endOffset, this)

    val underlyingType = unboxInlineClass()
    val defaultValueForUnderlyingType = IrConstImpl.defaultValueForType(startOffset, endOffset, underlyingType)
    return IrCallImpl(startOffset, endOffset, this, context.ir.symbols.unsafeCoerceIntrinsic).also {
        it.putTypeArgument(0, underlyingType) // from
        it.putTypeArgument(1, this) // to
        it.putValueArgument(0, defaultValueForUnderlyingType)
    }
}

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
    val parentDescriptor = propertyIfAccessor.parentAsClass.descriptor
    if (parentDescriptor !is DeserializedClassDescriptor) return jvmDefaultMode.forAllMethodsWithBody
    return JvmProtoBufUtil.isNewPlaceForBodyGeneration(parentDescriptor.classProto)
}

fun IrFunction.hasJvmDefault(): Boolean = propertyIfAccessor.hasAnnotation(JVM_DEFAULT_FQ_NAME)
fun IrClass.hasJvmDefaultNoCompatibilityAnnotation(): Boolean = hasAnnotation(JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME)
fun IrFunction.hasPlatformDependent(): Boolean = propertyIfAccessor.hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME)

fun IrFunction.getJvmVisibilityOfDefaultArgumentStub() =
    if (Visibilities.isPrivate(visibility) || isInlineOnly()) JavaVisibilities.PACKAGE_VISIBILITY else Visibilities.PUBLIC

fun IrValueParameter.isInlineParameter() =
    index >= 0 && !isNoinline && (type.isFunction() || type.isSuspendFunctionTypeOrSubtype()) &&
            // Parameters with default values are always nullable, so check the expression too.
            // Note that the frontend has a diagnostic for nullable inline parameters, so actually
            // making this return `false` requires using `@Suppress`.
            (!type.isNullable() || defaultValue?.expression?.type?.isNullable() == false)

val IrStatementOrigin?.isLambda: Boolean
    get() = this == IrStatementOrigin.LAMBDA || this == IrStatementOrigin.ANONYMOUS_FUNCTION

// An IR builder with a reference to the JvmBackendContext
class JvmIrBuilder(
    val backendContext: JvmBackendContext,
    val symbol: IrSymbol,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET
) : IrBuilderWithScope(
    IrLoweringContext(backendContext),
    Scope(symbol),
    startOffset,
    endOffset
) {
    val irSymbols: JvmSymbols
        get() = backendContext.ir.symbols
}

fun JvmBackendContext.createJvmIrBuilder(
    symbol: IrSymbol,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET
) = JvmIrBuilder(this, symbol, startOffset, endOffset)


fun IrDeclaration.isInCurrentModule(): Boolean =
    getPackageFragment() is IrFile

// Determine if the IrExpression is smartcast, and if so, if it is cast from higher than nullable target types.
// This is needed to pinpoint exceptional treatment of IEEE754 floating point comparisons, where proper IEEE
// comparisons are used "if values are statically known to be of primitive numeric types", taken to mean as
// "not learned through smartcasting".
fun IrExpression.isSmartcastFromHigherThanNullable(context: JvmBackendContext): Boolean {
    return when (this) {
        is IrTypeOperatorCall -> operator == IrTypeOperator.IMPLICIT_CAST && !argument.type.isSubtypeOf(
            type.makeNullable(),
            context.irBuiltIns
        )
        is IrGetValue -> {
            // Check if the variable initializer is smartcast. In FIR, if the subject of a `when` is smartcast,
            // the IMPLICIT_CAST is in the initializer of the variable for the subject.
            val variable = (symbol as? IrVariableSymbol)?.owner ?: return false
            !variable.isVar && variable.initializer?.isSmartcastFromHigherThanNullable(context) == true
        }
        else -> false
    }
}

fun IrBody.replaceThisByStaticReference(
    declarationFactory: JvmDeclarationFactory,
    irClass: IrClass,
    oldThisReceiverParameter: IrValueParameter
): IrBody =
    transform(object : IrElementTransformerVoid() {
        override fun visitGetValue(expression: IrGetValue): IrExpression {
            if (expression.symbol == oldThisReceiverParameter.symbol) {
                val instanceField = declarationFactory.getPrivateFieldForObjectInstance(irClass)
                return IrGetFieldImpl(
                    expression.startOffset,
                    expression.endOffset,
                    instanceField.symbol,
                    irClass.defaultType
                )
            }
            return super.visitGetValue(expression)
        }
    }, null)

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

fun createDelegatingCallWithPlaceholderTypeArguments(existingCall: IrCall, redirectTarget: IrFunction, irBuiltIns: IrBuiltIns): IrCall =
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

fun IrFunctionAccessExpression.copyFromWithPlaceholderTypeArguments(existingCall: IrFunctionAccessExpression, irBuiltIns: IrBuiltIns) {
    copyValueArgumentsFrom(
        existingCall,
        existingCall.symbol.owner,
        this.symbol.owner,
        receiversAsArguments = true,
        argumentsAsReceivers = false,
    )
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
    return override.overriddenSymbols.first {
        val owner = it.owner
        owner.modality != Modality.ABSTRACT && owner.overrides(implementation)
    }
}

// MethodSignatureMapper uses the corresponding property of a function to determine correct names
// for property accessors.
fun IrSimpleFunction.copyCorrespondingPropertyFrom(source: IrSimpleFunction) {
    val property = source.correspondingPropertySymbol?.owner ?: return
    val target = this

    correspondingPropertySymbol = buildProperty(property.symbol.descriptor) {
        name = property.name
        updateFrom(property)
    }.apply {
        parent = target.parent
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
    // @JvmField properties have no getters/setters
    resolveFakeOverride()?.backingField?.hasAnnotation(JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME) == true -> false
    // We do not produce default accessors for private fields
    else -> accessor.origin != IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR || !Visibilities.isPrivate(accessor.visibility)
}

val IrDeclaration.isStaticInlineClassReplacement: Boolean
    get() = origin == JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT
            || origin == JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_CONSTRUCTOR
