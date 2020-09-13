/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.KPropertyState
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.interpreter.toState
import kotlin.reflect.*

internal open class KProperty0Proxy(
    override val state: KPropertyState, override val interpreter: IrInterpreter
) : AbstractKPropertyProxy(state, interpreter), KProperty0<State> {
    override fun evaluate(expression: IrCall, args: List<Variable>): State {
        val result: Any = when (expression.symbol.owner.name.asString()) {
            "<get-name>" -> name
            "get" -> get()
            "invoke" -> invoke()
            else -> TODO("not supported expression for kProperty0")
        }

        return result.toState(expression.type)
    }

    override val getter: KProperty0.Getter<State>
        get() = TODO("Not yet implemented")
    override val parameters: List<KParameter>
        get() = TODO("Not yet implemented")
    override val typeParameters: List<KTypeParameter>
        get() = TODO("Not yet implemented")

    override fun call(vararg args: Any?): State {
        TODO("Not yet implemented")
    }

    override fun callBy(args: Map<KParameter, Any?>): State {
        TODO("Not yet implemented")
    }

    override fun get(): State {
        return state.dispatchReceiver!!.getState(state.propertyReference.symbol)!!
    }

    override fun getDelegate(): Any? {
        TODO("Not yet implemented")
    }

    override fun invoke(): State = get()
}

internal class KMutableProperty0Proxy(
    override val state: KPropertyState, override val interpreter: IrInterpreter
) : KProperty0Proxy(state, interpreter), KMutableProperty0<State> {
    override fun evaluate(expression: IrCall, args: List<Variable>): State {
        val result: Any = when (expression.symbol.owner.name.asString()) {
            "set" -> set(args[1].state)
            else -> return super.evaluate(expression, args)
        }

        return result.toState(expression.type)
    }

    override val setter: KMutableProperty0.Setter<State>
        get() = TODO("Not yet implemented")

    override fun set(value: State) {
        state.dispatchReceiver!!.setField(Variable(state.propertyReference.symbol, value))
    }
}
