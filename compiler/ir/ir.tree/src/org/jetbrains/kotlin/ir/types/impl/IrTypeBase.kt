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
) : IrTypeBase(kotlinType, annotations, variance), IrErrorType {
    override fun equals(other: Any?): Boolean = other is IrErrorTypeImpl

    override fun hashCode(): Int = IrErrorTypeImpl::class.java.name.hashCode()
}

class IrDynamicTypeImpl(
    kotlinType: KotlinType?,
    annotations: List<IrCall>,
    variance: Variance
) : IrTypeBase(kotlinType, annotations, variance), IrDynamicType, IrTypeProjection {
    override fun equals(other: Any?): Boolean = other is IrDynamicTypeImpl

    override fun hashCode(): Int = IrDynamicTypeImpl::class.java.name.hashCode()
}


val IrType.originalKotlinType: KotlinType?
    get() = safeAs<IrTypeBase>()?.kotlinType


object IrStarProjectionImpl : IrStarProjection {
    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}

@Deprecated("Hack to temporary cover late type initialization")
object IrUninitializedType : IrType {
    override val annotations: List<IrCall> = emptyList()

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}
