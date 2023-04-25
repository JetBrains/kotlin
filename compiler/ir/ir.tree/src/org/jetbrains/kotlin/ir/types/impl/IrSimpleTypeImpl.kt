/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.FqNameEqualityChecker
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.utils.compactIfPossible
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance

abstract class IrAbstractSimpleType(kotlinType: KotlinType?) : IrSimpleType(kotlinType) {

    override val variance: Variance
        get() = Variance.INVARIANT

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

abstract class IrDelegatedSimpleType(kotlinType: KotlinType? = null) : IrAbstractSimpleType(kotlinType) {

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

class IrSimpleTypeImpl(
    kotlinType: KotlinType?,
    override val classifier: IrClassifierSymbol,
    nullability: SimpleTypeNullability,
    override val arguments: List<IrTypeArgument>,
    override val annotations: List<IrConstructorCall>,
    override val abbreviation: IrTypeAbbreviation? = null
) : IrAbstractSimpleType(kotlinType) {

    override val nullability =
        if (classifier !is IrTypeParameterSymbol && nullability == SimpleTypeNullability.NOT_SPECIFIED)
            SimpleTypeNullability.DEFINITELY_NOT_NULL
        else
            nullability


    constructor(
        classifier: IrClassifierSymbol,
        nullability: SimpleTypeNullability,
        arguments: List<IrTypeArgument>,
        annotations: List<IrConstructorCall>,
        abbreviation: IrTypeAbbreviation? = null
    ) : this(null, classifier, nullability, arguments, annotations, abbreviation)

    constructor(
        classifier: IrClassifierSymbol,
        hasQuestionMark: Boolean,
        arguments: List<IrTypeArgument>,
        annotations: List<IrConstructorCall>,
        abbreviation: IrTypeAbbreviation? = null
    ) : this(null, classifier, SimpleTypeNullability.fromHasQuestionMark(hasQuestionMark), arguments, annotations, abbreviation)
}

class IrSimpleTypeBuilder {
    var kotlinType: KotlinType? = null
    var classifier: IrClassifierSymbol? = null
    var nullability = SimpleTypeNullability.NOT_SPECIFIED
    var arguments: List<IrTypeArgument> = emptyList()
    var annotations: List<IrConstructorCall> = emptyList()
    var abbreviation: IrTypeAbbreviation? = null
    var variance = Variance.INVARIANT
}

fun IrSimpleType.toBuilder() =
    IrSimpleTypeBuilder().also { b ->
        b.kotlinType = originalKotlinType
        b.classifier = classifier
        b.nullability = nullability
        b.arguments = arguments
        b.annotations = annotations
        b.abbreviation = abbreviation
    }

fun IrSimpleTypeBuilder.buildSimpleType() =
    IrSimpleTypeImpl(
        kotlinType,
        classifier ?: throw AssertionError("Classifier not provided"),
        nullability,
        arguments.compactIfPossible(),
        annotations.compactIfPossible(),
        abbreviation
    )

fun IrSimpleTypeBuilder.buildTypeProjection() =
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
        type is IrTypeProjection && type.variance == variance -> type
        type is IrSimpleType -> type.toBuilder().apply { this.variance = variance }.buildTypeProjection()
        type is IrDynamicType -> IrDynamicTypeImpl(null, type.annotations, variance)
        type is IrErrorType -> IrErrorTypeImpl(null, type.annotations, variance)
        else -> IrTypeProjectionImpl(type, variance)
    }


fun makeTypeIntersection(types: List<IrType>): IrType =
    with(types.map { makeTypeProjection(it, Variance.INVARIANT).type }.distinct()) {
        if (size == 1) return single()
        else firstOrNull { !(it.isAny() || it.isNullableAny()) } ?: first { it.isAny() }
    }
