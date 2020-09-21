/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state.reflection

import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeProxy
import org.jetbrains.kotlin.ir.interpreter.renderType
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.types.Variance
import kotlin.reflect.KTypeProjection

internal class KTypeState(val irType: IrType) : ReflectionState(irType.classifierOrFail) {
    private var _arguments: List<KTypeProjection>? = null

    fun getArguments(interpreter: IrInterpreter): List<KTypeProjection> {
        if (_arguments != null) return _arguments!!
        _arguments = (irType as IrSimpleType).arguments
            .map {
                when (it.getVariance()) {
                    Variance.INVARIANT -> KTypeProjection.invariant(KTypeProxy(KTypeState(it.typeOrNull!!), interpreter))
                    Variance.IN_VARIANCE -> KTypeProjection.contravariant(KTypeProxy(KTypeState(it.typeOrNull!!), interpreter))
                    Variance.OUT_VARIANCE -> KTypeProjection.covariant(KTypeProxy(KTypeState(it.typeOrNull!!), interpreter))
                    null -> KTypeProjection.STAR
                }
            }
        return _arguments!!
    }

    private fun IrTypeArgument.getVariance(): Variance? {
        return when (this) {
            is IrSimpleType -> Variance.INVARIANT
            is IrTypeProjection -> this.variance
            is IrStarProjection -> null
            else -> TODO()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KTypeState

        if (irType != other.irType) return false

        return true
    }

    override fun hashCode(): Int {
        return irType.hashCode()
    }

    override fun toString(): String {
        return irType.renderType()
    }
}