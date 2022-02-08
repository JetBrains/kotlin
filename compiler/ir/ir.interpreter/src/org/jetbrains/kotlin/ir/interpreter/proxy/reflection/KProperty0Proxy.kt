/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.ir.interpreter.CallInterceptor
import org.jetbrains.kotlin.ir.interpreter.exceptions.verify
import org.jetbrains.kotlin.ir.interpreter.getExtensionReceiver
import org.jetbrains.kotlin.ir.interpreter.state.isNull
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KPropertyState
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty0

internal open class KProperty0Proxy(
    state: KPropertyState, callInterceptor: CallInterceptor
) : AbstractKPropertyProxy(state, callInterceptor), KProperty0<Any?> {
    override val getter: KProperty0.Getter<Any?>
        get() = object : Getter(state.property.getter!!), KProperty0.Getter<Any?> {
            override fun invoke(): Any? = call()

            override fun call(vararg args: Any?): Any? {
                checkArguments(0, args.size)
                val actualPropertySymbol = state.property.resolveFakeOverride()?.symbol ?: state.property.symbol
                val receiver = state.receiver // null receiver <=> property is on top level
                    ?: return callInterceptor.interceptProxy(getter, emptyList())

                val value = receiver.getField(actualPropertySymbol)
                return when {
                    // null value <=> property is extension or Primitive; receiver.isNull() <=> nullable extension
                    value == null || receiver.isNull() -> callInterceptor.interceptProxy(getter, listOf(receiver))
                    else -> value
                }
            }

            override fun callBy(args: Map<KParameter, Any?>): Any? {
                TODO("Not yet implemented")
            }
        }

    override fun get(): Any? = getter.call()

    override fun getDelegate(): Any? {
        TODO("Not yet implemented")
    }

    override fun invoke(): Any? = getter.call()
}

internal class KMutableProperty0Proxy(
    state: KPropertyState, callInterceptor: CallInterceptor
) : KProperty0Proxy(state, callInterceptor), KMutableProperty0<Any?> {
    override val setter: KMutableProperty0.Setter<Any?> =
        object : Setter(state.property.setter!!), KMutableProperty0.Setter<Any?> {
            override fun invoke(p1: Any?) = call(p1)

            override fun call(vararg args: Any?) {
                checkArguments(1, args.size)
                // null receiver <=> property is on top level
                verify(state.receiver != null) { "Cannot interpret set method on top level properties" }
                verify(state.receiver?.irClass?.isObject != true) { "Cannot interpret set method on property of object" }
                val receiver = state.receiver!!
                val newValue = environment.convertToState(args.single(), propertyType)
                setter.getExtensionReceiver()
                    ?.let { callInterceptor.interceptProxy(setter, listOf(receiver, newValue)) }
                    ?: receiver.setField(state.property.symbol, newValue)
            }

            override fun callBy(args: Map<KParameter, Any?>) {
                TODO("Not yet implemented")
            }
        }

    override fun set(value: Any?) = setter.call(value)
}
