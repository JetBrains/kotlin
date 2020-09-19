/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.KPropertyState
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.interpreter.toState
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty0
import kotlin.reflect.KTypeParameter

internal open class KProperty0Proxy(
    override val state: KPropertyState, override val interpreter: IrInterpreter
) : AbstractKPropertyProxy(state, interpreter), KProperty0<Any?> {

    override val getter: KProperty0.Getter<State>
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

    override fun get(): Any? {
        return state.dispatchReceiver!!.getState(state.propertyReference.symbol)!!
    }

    override fun getDelegate(): Any? {
        TODO("Not yet implemented")
    }

    override fun invoke(): Any? = get()
}

internal class KMutableProperty0Proxy(
    override val state: KPropertyState, override val interpreter: IrInterpreter
) : KProperty0Proxy(state, interpreter), KMutableProperty0<Any?> {

    override val setter: KMutableProperty0.Setter<Any?>
        get() = TODO("Not yet implemented")

    override fun set(value: Any?) {
        state.dispatchReceiver!!.setField(Variable(state.propertyReference.symbol, value.toState(propertyType)))
    }
}
