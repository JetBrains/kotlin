/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.proxy.Proxy
import org.jetbrains.kotlin.ir.interpreter.proxy.wrap
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KPropertyState
import org.jetbrains.kotlin.ir.interpreter.toState
import kotlin.reflect.*

internal open class KProperty1Proxy(
    override val state: KPropertyState, override val interpreter: IrInterpreter
) : AbstractKPropertyProxy(state, interpreter), KProperty1<Proxy, Any?> {
    override val getter: KProperty1.Getter<Proxy, Any?>
        get() = object : Getter(state.property.getter!!), KProperty1.Getter<Proxy, Any?> {
            override fun invoke(p1: Proxy): Any? = call(p1)

            override fun call(vararg args: Any?): Any? {
                checkArguments(1, args.size)
                val receiver = args.single() as Proxy
                return receiver.state.getState(state.property.symbol)!!.wrap(interpreter)
            }

            override fun callBy(args: Map<KParameter, Any?>): Any? {
                TODO("Not yet implemented")
            }
        }

    override fun get(receiver: Proxy): Any? = getter.call(receiver)

    override fun getDelegate(receiver: Proxy): Any? {
        TODO("Not yet implemented")
    }

    override fun invoke(p1: Proxy): Any? = getter.call(p1)
}

internal class KMutableProperty1Proxy(
    override val state: KPropertyState, override val interpreter: IrInterpreter
) : KProperty1Proxy(state, interpreter), KMutableProperty1<Proxy, Any?> {
    override val setter: KMutableProperty1.Setter<Proxy, Any?> =
        object : Setter(state.property.setter!!), KMutableProperty1.Setter<Proxy, Any?> {
            override fun invoke(p1: Proxy, p2: Any?) = call(p1, p2)

            override fun call(vararg args: Any?) {
                checkArguments(2, args.size)
                val receiver = args[0] as Proxy
                val value = args[1]
                receiver.state.setField(Variable(state.property.symbol, value.toState(propertyType)))
            }

            override fun callBy(args: Map<KParameter, Any?>) {
                TODO("Not yet implemented")
            }
        }

    override fun set(receiver: Proxy, value: Any?) = setter.call(receiver, value)
}