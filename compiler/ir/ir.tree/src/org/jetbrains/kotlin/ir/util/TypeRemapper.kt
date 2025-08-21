/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.utils.memoryOptimizedMap

interface TypeRemapper {
    fun enterScope(irTypeParametersContainer: IrTypeParametersContainer)
    fun remapType(type: IrType): IrType
    fun leaveScope()
}

abstract class AbstractTypeRemapper() : TypeRemapper {
    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {}
    override fun leaveScope() {}

    // In all functions below, `null` means that the value has not changed after remapping and can be reused at the call site.
    protected abstract fun remapTypeOrNull(type: IrType): IrType?

    protected fun remapTypeArguments(arguments: List<IrTypeArgument>): List<IrTypeArgument>? {
        var anyChanged = false
        val result = arguments.memoryOptimizedMap {
            val remappedArgument = remapTypeArgument(it)
            if (remappedArgument != null) anyChanged = true
            remappedArgument ?: it
        }
        return if (anyChanged) result else null
    }

    protected fun remapTypeArgument(typeArgument: IrTypeArgument): IrTypeArgument? {
        return when (typeArgument) {
            is IrTypeProjection -> makeTypeProjection(this.remapTypeOrNull(typeArgument.type) ?: return null, typeArgument.variance)
            is IrStarProjection -> null
        }
    }

    final override fun remapType(type: IrType): IrType {
        return remapTypeOrNull(type) ?: type
    }

}

inline fun <T> TypeRemapper.withinScope(irTypeParametersContainer: IrTypeParametersContainer, fn: () -> T): T {
    enterScope(irTypeParametersContainer)
    val result = fn()
    leaveScope()
    return result
}