/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.stack

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.interpreter.Instruction
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterError
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

internal class Frame(subFrame: SubFrame, val irFile: IrFile? = null) {
    private val innerStack = mutableListOf(subFrame)
    private var currentInstruction: Instruction? = null

    private val currentFrame get() = innerStack.last()
    val currentSubFrameOwner: IrElement get() = currentFrame.owner

    companion object {
        const val NOT_DEFINED = "Not defined"
    }

    fun addSubFrame(frame: SubFrame) {
        innerStack.add(frame)
    }

    fun removeSubFrame() {
        currentFrame.peekState()?.let { if (innerStack.size > 1) innerStack[innerStack.size - 2].pushState(it) }
        removeSubFrameWithoutDataPropagation()
    }

    fun removeSubFrameWithoutDataPropagation() {
        innerStack.removeLast()
    }

    fun hasNoSubFrames() = innerStack.isEmpty()
    fun hasNoInstructions() = hasNoSubFrames() || (innerStack.size == 1 && innerStack.first().isEmpty())

    fun addInstruction(instruction: Instruction) {
        currentFrame.pushInstruction(instruction)
    }

    fun popInstruction(): Instruction {
        return currentFrame.popInstruction().apply { currentInstruction = this }
    }

    fun dropInstructions() = currentFrame.dropInstructions()

    fun pushState(state: State) {
        currentFrame.pushState(state)
    }

    fun popState(): State = currentFrame.popState()
    fun peekState(): State? = currentFrame.peekState()

    fun addVariable(variable: Variable) {
        currentFrame.addVariable(variable)
    }

    fun getState(symbol: IrSymbol): State {
        return (innerStack.lastIndex downTo 0).firstNotNullOfOrNull { innerStack[it].getState(symbol) }
            ?: throw InterpreterError("$symbol not found") // TODO better message
    }

    fun setState(symbol: IrSymbol, newState: State) {
        (innerStack.lastIndex downTo 0).forEach {
            if (innerStack[it].containsVariable(symbol))
                return innerStack[it].setState(symbol, newState)
        }
    }

    fun containsVariable(symbol: IrSymbol): Boolean = (innerStack.lastIndex downTo 0).any { innerStack[it].containsVariable(symbol) }

    fun getAll(): List<Variable> = innerStack.flatMap { it.getAll() }

    private fun getLineNumberForCurrentInstruction(): String {
        irFile ?: return ""
        val frameOwner = currentInstruction?.element
        return when {
            frameOwner is IrExpression || (frameOwner is IrDeclaration && frameOwner.origin == IrDeclarationOrigin.DEFINED) ->
                ":${irFile.fileEntry.getLineNumber(frameOwner.startOffset) + 1}"
            else -> ""
        }
    }

    fun getFileAndPositionInfo(): String {
        irFile ?: return NOT_DEFINED
        val lineNum = getLineNumberForCurrentInstruction()
        return "${irFile.name}$lineNum"
    }

    override fun toString(): String {
        irFile ?: return NOT_DEFINED
        val fileNameCapitalized = irFile.name.replace(".kt", "Kt").capitalizeAsciiOnly()
        val entryPoint = innerStack.firstOrNull { it.owner is IrFunction }?.owner as? IrFunction
        val lineNum = getLineNumberForCurrentInstruction()

        return "at $fileNameCapitalized.${entryPoint?.fqNameWhenAvailable ?: "<clinit>"}(${irFile.name}$lineNum)"
    }
}

internal class SubFrame(val owner: IrElement) {
    private val instructions = mutableListOf<Instruction>()
    private val dataStack = DataStack()
    private val memory = mutableListOf<Variable>()

    // Methods to work with instruction
    fun isEmpty() = instructions.isEmpty()
    fun pushInstruction(instruction: Instruction) = instructions.add(0, instruction)
    fun popInstruction(): Instruction = instructions.removeFirst()
    fun dropInstructions() = instructions.lastOrNull()?.apply { instructions.clear() }

    // Methods to work with data
    fun pushState(state: State) = dataStack.push(state)
    fun popState(): State = dataStack.pop()
    fun peekState(): State? = dataStack.peek()

    // Methods to work with memory
    fun addVariable(variable: Variable) {
        memory.add(0, variable)
    }

    private fun getVariable(symbol: IrSymbol): Variable? = memory.firstOrNull { it.symbol == symbol }
    fun containsVariable(symbol: IrSymbol): Boolean = getVariable(symbol) != null
    fun getState(symbol: IrSymbol): State? = getVariable(symbol)?.state
    fun setState(symbol: IrSymbol, newState: State) {
        getVariable(symbol)?.state = newState
    }

    fun getAll(): List<Variable> = memory
}

private class DataStack {
    private val stack = mutableListOf<State>()

    fun push(state: State) {
        stack.add(state)
    }

    fun pop(): State = stack.removeLast()
    fun peek(): State? = stack.lastOrNull()
}