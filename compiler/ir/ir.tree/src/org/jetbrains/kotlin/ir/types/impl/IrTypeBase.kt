/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.types.Variance

abstract class IrTypeBase(
    override val annotations: List<IrCall>,
    override val variance: Variance
) : IrType, IrTypeProjection {

    override val type: IrType get() = this

}

class IrErrorTypeImpl(
    annotations: List<IrCall>,
    variance: Variance
) : IrTypeBase(annotations, variance), IrErrorType

class IrDynamicTypeImpl(
    annotations: List<IrCall>,
    variance: Variance
) : IrTypeBase(annotations, variance), IrDynamicType, IrTypeProjection