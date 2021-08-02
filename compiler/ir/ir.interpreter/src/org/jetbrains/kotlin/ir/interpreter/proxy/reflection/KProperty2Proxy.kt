/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.ir.interpreter.CallInterceptor
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KPropertyState
import kotlin.reflect.*

internal open class KProperty2Proxy(
    state: KPropertyState, callInterceptor: CallInterceptor
) : AbstractKPropertyProxy(state, callInterceptor), KProperty2<Any?, Any?, Any?> {
    override val getter: KProperty2.Getter<Any?, Any?, Any?>
        get() = object : Getter(state.property.getter!!), KProperty2.Getter<Any?, Any?, Any?> {
            override fun invoke(p1: Any?, p2: Any?): Any? = call(p1, p2)

            override fun call(vararg args: Any?): Any? {
                checkArguments(2, args.size)
                val dispatchParameter = getter.dispatchReceiverParameter!!
                val extensionReceiverParameter = getter.extensionReceiverParameter!!
                val dispatch = Variable(dispatchParameter.symbol, environment.convertToState(args[0], dispatchParameter.type))
                val extension = Variable(extensionReceiverParameter.symbol, environment.convertToState(args[1], extensionReceiverParameter.type))
                return callInterceptor.interceptProxy(getter, listOf(dispatch, extension))
            }

            override fun callBy(args: Map<KParameter, Any?>): Any? {
                TODO("Not yet implemented")
            }
        }

    override fun get(receiver1: Any?, receiver2: Any?): Any? = getter.call(receiver1, receiver2)

    override fun getDelegate(receiver1: Any?, receiver2: Any?): Any? {
        TODO("Not yet implemented")
    }

    override fun invoke(p1: Any?, p2: Any?): Any? = getter.call(p1, p2)
}

internal class KMutableProperty2Proxy(
    state: KPropertyState, callInterceptor: CallInterceptor
) : KProperty2Proxy(state, callInterceptor), KMutableProperty2<Any?, Any?, Any?> {
    override val setter: KMutableProperty2.Setter<Any?, Any?, Any?>
        get() = object : Setter(state.property.setter!!), KMutableProperty2.Setter<Any?, Any?, Any?> {
            override fun invoke(p1: Any?, p2: Any?, p3: Any?) = call(p1, p2, p3)

            override fun call(vararg args: Any?) {
                checkArguments(3, args.size)
                val receiver1 = args[0]
                val receiver2 = args[1]
                val value = args[2]
                TODO("Not yet implemented, receiver1 = $receiver1, receiver2 = $receiver2, value = $value")
            }

            override fun callBy(args: Map<KParameter, Any?>) {
                TODO("Not yet implemented")
            }
        }

    override fun set(receiver1: Any?, receiver2: Any?, value: Any?) = setter.call(receiver1, receiver2, value)
}