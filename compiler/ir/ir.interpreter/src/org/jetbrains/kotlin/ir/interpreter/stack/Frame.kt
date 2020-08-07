/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.stack

import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterEmptyReturnStackError
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol

internal interface Frame {
    fun addVar(variable: Variable)
    fun addAll(variables: List<Variable>)
    fun getVariable(symbol: IrSymbol): Variable?
    fun getAll(): List<Variable>
    fun contains(symbol: IrSymbol): Boolean
    fun pushReturnValue(state: State)
    fun pushReturnValue(frame: Frame) // TODO rename to getReturnValueFrom
    fun peekReturnValue(): State
    fun popReturnValue(): State
    fun hasReturnValue(): Boolean
}

// TODO replace exceptions with InterpreterException
internal class InterpreterFrame(
    private val pool: MutableList<Variable> = mutableListOf(),
    private val typeArguments: List<Variable> = listOf()
) : Frame {
    private val returnStack: MutableList<State> = mutableListOf()

    override fun addVar(variable: Variable) {
        pool.add(variable)
    }

    override fun addAll(variables: List<Variable>) {
        pool.addAll(variables)
    }

    override fun getVariable(symbol: IrSymbol): Variable? {
        return (if (symbol is IrTypeParameterSymbol) typeArguments else pool).firstOrNull { it.symbol == symbol }
    }

    override fun getAll(): List<Variable> {
        return pool
    }

    override fun contains(symbol: IrSymbol): Boolean {
        return (typeArguments + pool).any { it.symbol == symbol }
    }

    override fun pushReturnValue(state: State) {
        returnStack += state
    }

    override fun pushReturnValue(frame: Frame) {
        if (frame.hasReturnValue()) this.pushReturnValue(frame.popReturnValue())
    }

    override fun hasReturnValue(): Boolean {
        return returnStack.isNotEmpty()
    }

    override fun peekReturnValue(): State {
        if (returnStack.isNotEmpty()) {
            return returnStack.last()
        }
        throw InterpreterEmptyReturnStackError()
    }

    override fun popReturnValue(): State {
        if (returnStack.isNotEmpty()) {
            val item = returnStack.last()
            returnStack.removeAt(returnStack.size - 1)
            return item
        }
        throw InterpreterEmptyReturnStackError()
    }
}
