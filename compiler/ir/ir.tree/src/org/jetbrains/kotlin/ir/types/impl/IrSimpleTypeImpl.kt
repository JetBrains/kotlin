/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.types.Variance

class IrSimpleTypeImpl(
    override val classifier: IrClassifierSymbol,
    override val hasQuestionMark: Boolean,
    override val arguments: List<IrTypeProjection>,
    annotations: List<IrCall>,
    variance: Variance
) : IrTypeBase(annotations, variance), IrSimpleType, IrTypeProjection {

    constructor(
        classifier: IrClassifierSymbol,
        hasQuestionMark: Boolean,
        arguments: List<IrTypeProjection>,
        annotations: List<IrCall>
    ) : this(classifier, hasQuestionMark, arguments, annotations, Variance.INVARIANT)

    constructor(
        other: IrSimpleType,
        variance: Variance
    ) : this(other.classifier, other.hasQuestionMark, other.arguments, other.annotations, variance)

}

class IrTypeProjectionImpl internal constructor(
    override val type: IrType,
    override val variance: Variance
) : IrTypeProjection

fun makeTypeProjection(type: IrType, variance: Variance): IrTypeProjection =
    when {
        type is IrTypeProjection && type.variance == variance -> type
        type is IrSimpleType -> IrSimpleTypeImpl(type, variance)
        type is IrDynamicType -> IrDynamicTypeImpl(type.annotations, variance)
        type is IrErrorType -> IrErrorTypeImpl(type.annotations, variance)
        else -> IrTypeProjectionImpl(type, variance)
    }