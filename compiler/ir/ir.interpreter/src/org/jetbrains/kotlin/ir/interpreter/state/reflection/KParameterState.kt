/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state.reflection

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.interpreter.CallInterceptor
import org.jetbrains.kotlin.ir.interpreter.property
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeProxy
import kotlin.reflect.KParameter
import kotlin.reflect.KType

internal class KParameterState(
    override val irClass: IrClass, val irParameter: IrValueParameter, val index: Int, val kind: KParameter.Kind = KParameter.Kind.VALUE
) : ReflectionState() {
    private var _type: KType? = null

    fun getType(callInterceptor: CallInterceptor): KType {
        if (_type != null) return _type!!
        val kTypeIrClass = callInterceptor.environment.kTypeClass.owner
        _type = KTypeProxy(KTypeState(irParameter.type, kTypeIrClass), callInterceptor)
        return _type!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KParameterState

        if (irParameter != other.irParameter) return false

        return true
    }

    override fun hashCode(): Int {
        return irParameter.hashCode()
    }

    override fun toString(): String {
        return buildString {
            when (kind) {
                KParameter.Kind.EXTENSION_RECEIVER -> append("extension receiver parameter")
                KParameter.Kind.INSTANCE -> append("instance parameter")
                KParameter.Kind.VALUE -> append("parameter #$index ${irParameter.name}")
            }

            append(" of ")
            when (val parent = irParameter.parent) {
                is IrSimpleFunction -> parent.property?.let { append(renderProperty(it)) }
                    ?: append(renderFunction(parent))
                is IrFunction -> append(renderFunction(parent))
                is IrProperty -> append(renderProperty(parent))
                else -> TODO()
            }
        }
    }
}