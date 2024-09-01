/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.FqNameEqualityChecker
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.utils.compactIfPossible

abstract class IrAbstractSimpleType : IrSimpleType() {
    abstract override val classifier: IrClassifierSymbol
    abstract override val nullability: SimpleTypeNullability
    abstract override val arguments: List<IrTypeArgument>
    abstract override val annotations: List<IrConstructorCall>
    abstract override val abbreviation: IrTypeAbbreviation?

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

abstract class IrDelegatedSimpleType : IrAbstractSimpleType() {

    protected abstract val delegate: IrSimpleType

    override val classifier: IrClassifierSymbol
        get() = delegate.classifier
    override val nullability: SimpleTypeNullability
        get() = delegate.nullability
    override val arguments: List<IrTypeArgument>
        get() = delegate.arguments
    override val abbreviation: IrTypeAbbreviation?
        get() = delegate.abbreviation
    override val annotations: List<IrConstructorCall>
        get() = delegate.annotations
}

open class IrSimpleTypeImpl(
    final override val classifier: IrClassifierSymbol,
    nullability: SimpleTypeNullability,
    final override val arguments: List<IrTypeArgument>,
    final override val annotations: List<IrConstructorCall>,
    final override val abbreviation: IrTypeAbbreviation? = null,
) : IrAbstractSimpleType() {

    final override val nullability =
        if (classifier !is IrTypeParameterSymbol && nullability == SimpleTypeNullability.NOT_SPECIFIED)
            SimpleTypeNullability.DEFINITELY_NOT_NULL
        else
            nullability

    constructor(
        classifier: IrClassifierSymbol,
        hasQuestionMark: Boolean,
        arguments: List<IrTypeArgument>,
        annotations: List<IrConstructorCall>,
        abbreviation: IrTypeAbbreviation? = null
    ) : this(classifier, SimpleTypeNullability.fromHasQuestionMark(hasQuestionMark), arguments, annotations, abbreviation)
}

class IrSimpleTypeWithOriginalKotlinTypeImpl(
    override val originalKotlinType: KotlinType,
    classifier: IrClassifierSymbol,
    nullability: SimpleTypeNullability,
    arguments: List<IrTypeArgument>,
    annotations: List<IrConstructorCall>,
    abbreviation: IrTypeAbbreviation? = null,
) : IrSimpleTypeImpl(classifier, nullability, arguments, annotations, abbreviation)

class IrSimpleTypeBuilder {
    var kotlinType: KotlinType? = null
    var classifier: IrClassifierSymbol? = null
    var nullability = SimpleTypeNullability.NOT_SPECIFIED
    var arguments: List<IrTypeArgument> = emptyList()
    var annotations: List<IrConstructorCall> = emptyList()
    var abbreviation: IrTypeAbbreviation? = null

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
        b.abbreviation = abbreviation
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
            abbreviation,
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
        if (kotlinType == null) {
            IrSimpleTypeImpl(
                classifier,
                nullability,
                arguments,
                annotations,
                abbreviation,
            )
        } else {
            IrSimpleTypeWithOriginalKotlinTypeImpl(
                kotlinType,
                classifier,
                nullability,
                arguments,
                annotations,
                abbreviation,
            )
        }
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
