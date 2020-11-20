/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

abstract class IrTypeBase(val kotlinType: KotlinType?) : IrType, IrTypeProjection {
    override val type: IrType get() = this
}

class IrErrorTypeImpl(
    kotlinType: KotlinType?,
    override val annotations: List<IrConstructorCall>,
    override val variance: Variance,
) : IrTypeBase(kotlinType), IrErrorType {
    override fun equals(other: Any?): Boolean = other is IrErrorTypeImpl

    override fun hashCode(): Int = IrErrorTypeImpl::class.java.hashCode()
}

class IrDynamicTypeImpl(
    kotlinType: KotlinType?,
    override val annotations: List<IrConstructorCall>,
    override val variance: Variance,
) : IrTypeBase(kotlinType), IrDynamicType {
    override fun equals(other: Any?): Boolean = other is IrDynamicTypeImpl

    override fun hashCode(): Int = IrDynamicTypeImpl::class.java.hashCode()
}


val IrType.originalKotlinType: KotlinType?
    get() = safeAs<IrTypeBase>()?.kotlinType


object IrStarProjectionImpl : IrStarProjection {
    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * An instance which should be used when creating an IR element whose type cannot be determined at the moment of creation.
 *
 * Example: when translating generic functions in psi2ir, we're creating an IrFunction first, then adding IrTypeParameter instances to it,
 * and only then translating the function's return type with respect to those created type parameters.
 *
 * Instead of using this special instance, we could just make IrFunction/IrConstructor constructors allow to accept no return type,
 * however this could lead to a situation where we forget to set return type sometimes. This would result in crashes at unexpected moments,
 * especially in Kotlin/JS where function return types are not present in the resulting binary files.
 */
object IrUninitializedType : IrType {
    override val annotations: List<IrConstructorCall> = emptyList()

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}

class ReturnTypeIsNotInitializedException(function: IrFunction) : IllegalStateException(
    "Return type is not initialized for function '${function.name}'"
)
