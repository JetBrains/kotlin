/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.types.Variance

class IrTypeImpl(
    override val classifier: IrClassifierSymbol,
    override val hasQuestionMark: Boolean,
    override val arguments: List<IrTypeProjection>,
    override val annotations: List<IrCall>,
    override val variance: Variance
) : IrType, IrTypeProjection {

    constructor(
        classifier: IrClassifierSymbol,
        hasQuestionMark: Boolean,
        arguments: List<IrTypeProjection>,
        annotations: List<IrCall>
    ) : this(classifier, hasQuestionMark, arguments, annotations, Variance.INVARIANT)

    constructor(
        other: IrType,
        variance: Variance
    ) : this(other.classifier, other.hasQuestionMark, other.arguments, other.annotations, variance)

    override val type: IrType get() = this
}

fun makeTypeProjection(type: IrType, variance: Variance): IrTypeProjection =
    if (type is IrTypeImpl && type.variance == variance)
        type
    else
        IrTypeImpl(type, variance)