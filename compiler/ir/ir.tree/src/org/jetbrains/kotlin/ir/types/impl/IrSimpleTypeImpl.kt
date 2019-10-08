/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.FqNameEqualityChecker
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance

class IrSimpleTypeImpl(
    kotlinType: KotlinType?,
    override val classifier: IrClassifierSymbol,
    override val hasQuestionMark: Boolean,
    override val arguments: List<IrTypeArgument>,
    annotations: List<IrConstructorCall>,
    override val abbreviation: IrTypeAbbreviation? = null
) : IrTypeBase(kotlinType, annotations, Variance.INVARIANT), IrSimpleType, IrTypeProjection {

    constructor(
        classifier: IrClassifierSymbol,
        hasQuestionMark: Boolean,
        arguments: List<IrTypeArgument>,
        annotations: List<IrConstructorCall>,
        abbreviation: IrTypeAbbreviation? = null
    ) : this(null, classifier, hasQuestionMark, arguments, annotations, abbreviation)

    override fun equals(other: Any?): Boolean =
        other is IrSimpleTypeImpl &&
                FqNameEqualityChecker.areEqual(classifier, other.classifier) &&
                hasQuestionMark == other.hasQuestionMark &&
                arguments == other.arguments

    override fun hashCode(): Int =
        (FqNameEqualityChecker.getHashCode(classifier) * 31 +
                hasQuestionMark.hashCode()) * 31 +
                arguments.hashCode()
}

class IrSimpleTypeBuilder {
    var kotlinType: KotlinType? = null
    var classifier: IrClassifierSymbol? = null
    var hasQuestionMark = false
    var arguments: List<IrTypeArgument> = emptyList()
    var annotations: List<IrConstructorCall> = emptyList()
    var abbreviation: IrTypeAbbreviation? = null
    var variance = Variance.INVARIANT
}

fun IrSimpleType.toBuilder() =
    IrSimpleTypeBuilder().also { b ->
        b.kotlinType = originalKotlinType
        b.classifier = classifier
        b.hasQuestionMark = hasQuestionMark
        b.arguments = arguments
        b.annotations = annotations
        b.abbreviation = abbreviation
    }

fun IrSimpleTypeBuilder.buildSimpleType() =
    IrSimpleTypeImpl(
        kotlinType,
        classifier ?: throw AssertionError("Classifier not provided"),
        hasQuestionMark,
        arguments,
        annotations,
        abbreviation
    )

fun IrSimpleTypeBuilder.buildTypeProjection() =
    if (variance == Variance.INVARIANT)
        buildSimpleType()
    else
        IrTypeProjectionImpl(buildSimpleType(), variance)

inline fun IrSimpleType.buildSimpleType(b: IrSimpleTypeBuilder.() -> Unit): IrSimpleType =
    toBuilder().apply(b).buildSimpleType()

inline fun IrSimpleType.buildTypeProjection(b: IrSimpleTypeBuilder.() -> Unit): IrTypeProjection =
    toBuilder().apply(b).buildTypeProjection()

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