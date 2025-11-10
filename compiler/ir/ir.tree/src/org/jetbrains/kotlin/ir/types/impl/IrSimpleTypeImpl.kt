/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.symbols.FqNameEqualityChecker
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.utils.compactIfPossible

abstract class IrAbstractSimpleType(
    final override val classifier: IrClassifierSymbol,
    final override val nullability: SimpleTypeNullability,
) : IrSimpleType() {

    override fun equals(other: Any?): Boolean =
        other is IrAbstractSimpleType &&
                FqNameEqualityChecker.areEqual(classifier, other.classifier) &&
                nullability == other.nullability &&
                arguments == other.arguments

    override fun hashCode(): Int =
        (FqNameEqualityChecker.getHashCode(classifier) * 31 +
                nullability.hashCode()) * 31 +
                arguments.hashCode()
}

abstract class IrDelegatedSimpleType : IrSimpleType() {

    protected abstract val delegate: IrSimpleType

    override val classifier: IrClassifierSymbol
        get() = delegate.classifier
    override val nullability: SimpleTypeNullability
        get() = delegate.nullability
    override val arguments: List<IrTypeArgument>
        get() = delegate.arguments
    override val annotations: List<IrAnnotation>
        get() = delegate.annotations

    override fun equals(other: Any?): Boolean = delegate == other
    override fun hashCode(): Int = delegate.hashCode()
    override fun toString(): String = delegate.toString()
}

private class IrSimpleTypeImpl(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    classifier: IrClassifierSymbol,
    nullability: SimpleTypeNullability,
    override val arguments: List<IrTypeArgument>,
    override val annotations: List<IrAnnotation>,
) : IrAbstractSimpleType(classifier, nullability) {
}

private class IrSimpleTypeOnlyClassifierImpl(
    classifier: IrClassifierSymbol,
    nullability: SimpleTypeNullability,
) : IrAbstractSimpleType(classifier, nullability) {
    override val annotations: List<IrAnnotation> get() = emptyList()
    override val arguments: List<IrTypeArgument> get() = emptyList()
}

private class IrSimpleTypeFullImpl(
    classifier: IrClassifierSymbol,
    nullability: SimpleTypeNullability,
    override val arguments: List<IrTypeArgument>,
    override val annotations: List<IrAnnotation>,
    override val originalKotlinType: KotlinType?,
) : IrAbstractSimpleType(classifier, nullability)


fun IrSimpleTypeImpl(
    classifier: IrClassifierSymbol,
    hasQuestionMark: Boolean,
    arguments: List<IrTypeArgument>,
    annotations: List<IrAnnotation>
): IrSimpleType = IrSimpleTypeImpl(
    classifier, SimpleTypeNullability.fromHasQuestionMark(hasQuestionMark), arguments, annotations
)

fun IrSimpleTypeImpl(
    classifier: IrClassifierSymbol,
    nullability: SimpleTypeNullability,
    arguments: List<IrTypeArgument>,
    annotations: List<IrAnnotation>,
    originalKotlinType: KotlinType? = null,
): IrSimpleType {
    val realNullability = if (classifier !is IrTypeParameterSymbol && nullability == SimpleTypeNullability.NOT_SPECIFIED)
        SimpleTypeNullability.DEFINITELY_NOT_NULL
    else
        nullability
    return when {
        originalKotlinType != null -> {
            IrSimpleTypeFullImpl(
                classifier,
                realNullability,
                arguments.compactIfPossible(),
                annotations.compactIfPossible(),
                originalKotlinType,
            )
        }
        annotations.isEmpty() && arguments.isEmpty() -> {
            IrSimpleTypeOnlyClassifierImpl(
                classifier,
                realNullability,
            )
        }
        else -> {
            IrSimpleTypeImpl(
                constructorIndicator = null,
                classifier,
                realNullability,
                arguments.compactIfPossible(),
                annotations.compactIfPossible(),
            )
        }
    }
}

class IrSimpleTypeBuilder {
    var kotlinType: KotlinType? = null
    var classifier: IrClassifierSymbol? = null
    var nullability = SimpleTypeNullability.NOT_SPECIFIED
    var arguments: List<IrTypeArgument> = emptyList()
    var annotations: List<IrAnnotation> = emptyList()

    var captureStatus: CaptureStatus? = null
    var capturedLowerType: IrType? = null
    var capturedTypeConstructor: IrCapturedType.Constructor? = null
}

fun IrSimpleType.toBuilder(): IrSimpleTypeBuilder =
    IrSimpleTypeBuilder().also { b ->
        b.kotlinType = originalKotlinType
        if (this is IrCapturedType) {
            b.captureStatus = captureStatus
            b.capturedLowerType = lowerType
            b.capturedTypeConstructor = constructor
        } else {
            b.classifier = classifier
        }
        b.nullability = nullability
        b.arguments = arguments
        b.annotations = annotations
    }

fun IrSimpleTypeBuilder.buildSimpleType(): IrSimpleType =
    if (classifier == null) {
        check(captureStatus != null && capturedTypeConstructor != null) {
            "Neither classifier nor captured type constructor is provided"
        }
        check(arguments.isEmpty()) {
            "Arguments should be empty when creating a captured type: ${capturedTypeConstructor?.argument?.render()}"
        }
        IrCapturedType(
            captureStatus!!,
            capturedLowerType,
            capturedTypeConstructor!!.argument,
            capturedTypeConstructor!!.typeParameter,
            nullability,
            annotations.compactIfPossible(),
        ).apply {
            constructor.initSuperTypes(capturedTypeConstructor!!.superTypes)
        }
    } else {
        check(captureStatus == null && capturedTypeConstructor == null) {
            "Both classifier and captured type constructor are provided"
        }
        val kotlinType = this.kotlinType
        val classifier = this.classifier ?: throw AssertionError("Classifier not provided")
        val arguments = this.arguments.compactIfPossible()
        val annotations = this.annotations.compactIfPossible()
        IrSimpleTypeImpl(
            classifier,
            nullability,
            arguments,
            annotations,
            originalKotlinType = kotlinType
        )
    }

fun IrSimpleTypeBuilder.buildTypeProjection(variance: Variance): IrTypeProjection =
    if (variance == Variance.INVARIANT)
        buildSimpleType()
    else
        IrTypeProjectionImpl(buildSimpleType(), variance)

inline fun IrSimpleType.buildSimpleType(b: IrSimpleTypeBuilder.() -> Unit): IrSimpleType =
    toBuilder().apply(b).buildSimpleType()

class IrTypeProjectionImpl internal constructor(
    override val type: IrType,
    override val variance: Variance
) : IrTypeProjection {
    override fun equals(other: Any?): Boolean =
        other is IrTypeProjectionImpl && type == other.type && variance == other.variance

    override fun hashCode(): Int =
        type.hashCode() * 31 + variance.hashCode()
}

fun makeTypeProjection(type: IrType, variance: Variance): IrTypeProjection =
    when {
        type is IrCapturedType -> IrTypeProjectionImpl(type, variance)
        type.variance == variance -> type
        type is IrSimpleType -> type.toBuilder().buildTypeProjection(variance)
        type is IrDynamicType -> IrDynamicTypeImpl(type.annotations, variance)
        type is IrErrorType -> IrErrorTypeImpl(type.originalKotlinType, type.annotations, variance)
        else -> IrTypeProjectionImpl(type, variance)
    }

fun makeTypeIntersection(types: Collection<IrType>): IrType =
    with(types.map { makeTypeProjection(it, Variance.INVARIANT).type }.distinct()) {
        if (size == 1) return single()
        else firstOrNull { !(it.isAny() || it.isNullableAny()) } ?: first { it.isAny() }
    }
