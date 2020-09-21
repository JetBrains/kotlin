/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.state.KTypeParameterState
import org.jetbrains.kotlin.types.Variance
import kotlin.reflect.*

internal class KTypeParameterProxy(
    override val state: KTypeParameterState, override val interpreter: IrInterpreter
) : ReflectionProxy, KTypeParameter {
    override val name: String
        get() = state.irTypeParameter.name.asString()
    override val upperBounds: List<KType>
        get() = state.getUpperBounds(interpreter)
    override val variance: KVariance
        get() = when (state.irTypeParameter.variance) {
            Variance.INVARIANT -> KVariance.INVARIANT
            Variance.IN_VARIANCE -> KVariance.IN
            Variance.OUT_VARIANCE -> KVariance.OUT
        }
    override val isReified: Boolean
        get() = state.irTypeParameter.isReified

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KTypeParameterProxy) return false

        return state == other.state
    }

    override fun hashCode(): Int = state.hashCode()

    override fun toString(): String = state.toString()
}