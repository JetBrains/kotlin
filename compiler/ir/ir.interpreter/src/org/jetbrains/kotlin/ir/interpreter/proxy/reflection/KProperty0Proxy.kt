/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.ir.interpreter.CallInterceptor
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KPropertyState
import org.jetbrains.kotlin.ir.interpreter.toState
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import kotlin.reflect.*

internal open class KProperty0Proxy(
    override val state: KPropertyState, override val callInterceptor: CallInterceptor
) : AbstractKPropertyProxy(state, callInterceptor), KProperty0<Any?> {
    override val getter: KProperty0.Getter<Any?>
        get() = object : Getter(state.property.getter!!), KProperty0.Getter<Any?> {
            override fun invoke(): Any? = call()

            override fun call(vararg args: Any?): Any? {
                checkArguments(0, args.size)
                val actualPropertySymbol = state.property.resolveFakeOverride()?.symbol ?: state.property.symbol
                return state.dispatchReceiver!!.getState(actualPropertySymbol)!!
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
    override val state: KPropertyState, override val callInterceptor: CallInterceptor
) : KProperty0Proxy(state, callInterceptor), KMutableProperty0<Any?> {
    override val setter: KMutableProperty0.Setter<Any?> =
        object : Setter(state.property.setter!!), KMutableProperty0.Setter<Any?> {
            override fun invoke(p1: Any?) = call(p1)

            override fun call(vararg args: Any?) {
                checkArguments(1, args.size)
                state.dispatchReceiver!!.setField(Variable(state.property.symbol, args.single().toState(propertyType)))
            }

            override fun callBy(args: Map<KParameter, Any?>) {
                TODO("Not yet implemented")
            }
        }

    override fun set(value: Any?) = setter.call(value)
}
