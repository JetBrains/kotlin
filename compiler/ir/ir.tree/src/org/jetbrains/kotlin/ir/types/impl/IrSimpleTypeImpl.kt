/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.FqNameEqualityChecker
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class IrSimpleTypeImpl(
    kotlinType: KotlinType?,
    override val classifier: IrClassifierSymbol,
    override val hasQuestionMark: Boolean,
    override val arguments: List<IrTypeArgument>,
    annotations: List<IrCall>,
    variance: Variance
) : IrTypeBase(kotlinType, annotations, variance), IrSimpleType, IrTypeProjection {

    constructor(
        classifier: IrClassifierSymbol,
        hasQuestionMark: Boolean,
        arguments: List<IrTypeArgument>,
        annotations: List<IrCall>
    ) : this(null, classifier, hasQuestionMark, arguments, annotations, Variance.INVARIANT)

    constructor(
        kotlinType: KotlinType?,
        classifier: IrClassifierSymbol,
        hasQuestionMark: Boolean,
        arguments: List<IrTypeArgument>,
        annotations: List<IrCall>
    ) : this(kotlinType, classifier, hasQuestionMark, arguments, annotations, Variance.INVARIANT)

    constructor(
        other: IrSimpleType,
        variance: Variance
    ) :
            this(
                other.safeAs<IrSimpleTypeImpl>()?.kotlinType,
                other.classifier, other.hasQuestionMark, other.arguments, other.annotations, variance
            )

    override fun equals(other: Any?): Boolean =
        other is IrSimpleTypeImpl &&
                FqNameEqualityChecker.areEqual(classifier, other.classifier) &&
                hasQuestionMark == other.hasQuestionMark &&
                arguments == other.arguments &&
                variance == other.variance

    override fun hashCode(): Int =
        ((FqNameEqualityChecker.getHashCode(classifier) * 31 + hasQuestionMark.hashCode()) * 31 +
                arguments.hashCode()) * 31 + variance.hashCode()
}

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
        type is IrSimpleType -> IrSimpleTypeImpl(type, variance)
        type is IrDynamicType -> IrDynamicTypeImpl(null, type.annotations, variance)
        type is IrErrorType -> IrErrorTypeImpl(null, type.annotations, variance)
        else -> IrTypeProjectionImpl(type, variance)
    }
