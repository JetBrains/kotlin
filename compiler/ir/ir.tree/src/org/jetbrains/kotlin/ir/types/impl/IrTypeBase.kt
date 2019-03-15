/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

abstract class IrTypeBase(
    val kotlinType: KotlinType?,
    override val annotations: List<IrCall>,
    override val variance: Variance
) : IrType, IrTypeProjection {

    override val type: IrType get() = this

}

class IrErrorTypeImpl(
    kotlinType: KotlinType?,
    annotations: List<IrCall>,
    variance: Variance
) : IrTypeBase(kotlinType, annotations, variance), IrErrorType

class IrDynamicTypeImpl(
    kotlinType: KotlinType?,
    annotations: List<IrCall>,
    variance: Variance
) : IrTypeBase(kotlinType, annotations, variance), IrDynamicType, IrTypeProjection


val IrType.originalKotlinType: KotlinType?
    get() = safeAs<IrTypeBase>()?.kotlinType


object IrStarProjectionImpl : IrStarProjection

@Deprecated("Hack to temporary cover late type initialization")
object IrUninitializedType : IrType {
    override val annotations: List<IrCall> = emptyList()
}
