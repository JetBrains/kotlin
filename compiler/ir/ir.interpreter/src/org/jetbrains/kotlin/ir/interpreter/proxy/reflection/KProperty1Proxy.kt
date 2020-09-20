/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.proxy.Proxy
import org.jetbrains.kotlin.ir.interpreter.proxy.wrap
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.KPropertyState
import org.jetbrains.kotlin.ir.interpreter.toState
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KTypeParameter

internal open class KProperty1Proxy(
    override val state: KPropertyState, override val interpreter: IrInterpreter
) : AbstractKPropertyProxy(state, interpreter), KProperty1<Proxy, Any?> {

    override val getter: KProperty1.Getter<Proxy, Any?>
        get() = TODO("Not yet implemented")
    override val parameters: List<KParameter>
        get() = TODO("Not yet implemented")
    override val typeParameters: List<KTypeParameter>
        get() = TODO("Not yet implemented")

    override fun call(vararg args: Any?): Any? {
        TODO("Not yet implemented")
    }

    override fun callBy(args: Map<KParameter, Any?>): Any? {
        TODO("Not yet implemented")
    }

    override fun get(receiver: Proxy): Any? {
        return receiver.state.getState(state.property.symbol)!!.wrap(interpreter)
    }

    override fun getDelegate(receiver: Proxy): Any? {
        TODO("Not yet implemented")
    }

    override fun invoke(p1: Proxy): Any? = get(p1)
}

internal class KMutableProperty1Proxy(
    override val state: KPropertyState, override val interpreter: IrInterpreter
) : KProperty1Proxy(state, interpreter), KMutableProperty1<Proxy, Any?> {

    override val setter: KMutableProperty1.Setter<Proxy, Any?>
        get() = TODO("Not yet implemented")

    override fun set(receiver: Proxy, value: Any?) {
        receiver.state.setField(Variable(state.property.symbol, value.toState(propertyType)))
    }
}