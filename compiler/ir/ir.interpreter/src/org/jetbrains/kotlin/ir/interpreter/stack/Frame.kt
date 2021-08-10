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
import org.jetbrains.kotlin.ir.interpreter.state.StateWithClosure
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

internal class Frame(subFrameOwner: IrElement, val irFile: IrFile? = null) {
    private val innerStack = ArrayDeque<SubFrame>().apply { add(SubFrame(subFrameOwner)) }
    private var currentInstruction: Instruction? = null

    private val currentFrame get() = innerStack.last()
    val currentSubFrameOwner: IrElement get() = currentFrame.owner

    companion object {
        const val NOT_DEFINED = "Not defined"
    }

    fun addSubFrame(subFrameOwner: IrElement) {
        innerStack.add(SubFrame(subFrameOwner))
    }

    fun removeSubFrame() {
        val state = currentFrame.peekState()
        removeSubFrameWithoutDataPropagation()
        if (!hasNoSubFrames() && state != null) currentFrame.pushState(state)
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

    fun addVariable(symbol: IrSymbol, state: State?) {
        currentFrame.addVariable(symbol, state)
    }

    fun addVariable(symbol: IrSymbol, variable: Variable) {
        currentFrame.addVariable(symbol, variable)
    }

    private inline fun forEachSubFrame(block: (SubFrame) -> Unit) {
        // TODO speed up reverse iteration or do it forward
        (innerStack.lastIndex downTo 0).forEach {
            block(innerStack[it])
        }
    }

    fun getState(symbol: IrSymbol): State {
        forEachSubFrame {
            it.getState(symbol)?.let { state -> return state }
        }
        throw InterpreterError("$symbol not found") // TODO better message
    }

    fun setState(symbol: IrSymbol, newState: State) {
        forEachSubFrame {
            if (it.containsVariable(symbol)) return it.setState(symbol, newState)
        }
    }

    fun containsVariable(symbol: IrSymbol): Boolean {
        forEachSubFrame {
            if (it.containsVariable(symbol)) return true
        }
        return false
    }

    fun copyMemoryInto(newFrame: Frame) {
        this.getAll().forEach { (symbol, variable) -> if (!newFrame.containsVariable(symbol)) newFrame.addVariable(symbol, variable) }
    }

    fun copyMemoryInto(closure: StateWithClosure) {
        getAll().reversed().forEach { (symbol, variable) -> closure.upValues[symbol] = variable }
    }

    private fun getAll(): List<Pair<IrSymbol, Variable>> = innerStack.flatMap { it.getAll() }

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

private class SubFrame(val owner: IrElement) {
    private val instructions = ArrayDeque<Instruction>()
    private val dataStack = DataStack()
    private val memory = mutableMapOf<IrSymbol, Variable>()

    // Methods to work with instruction
    fun isEmpty() = instructions.isEmpty()
    fun pushInstruction(instruction: Instruction) = instructions.addFirst(instruction)
    fun popInstruction(): Instruction = instructions.removeFirst()
    fun dropInstructions() = instructions.lastOrNull()?.apply { instructions.clear() }

    // Methods to work with data
    fun pushState(state: State) = dataStack.push(state)
    fun popState(): State = dataStack.pop()
    fun peekState(): State? = dataStack.peek()

    // Methods to work with memory
    fun addVariable(symbol: IrSymbol, variable: Variable) {
        memory[symbol] = variable
    }

    fun addVariable(symbol: IrSymbol, state: State?) {
        memory[symbol] = Variable(state)
    }

    fun containsVariable(symbol: IrSymbol): Boolean = memory[symbol] != null
    fun getState(symbol: IrSymbol): State? = memory[symbol]?.state
    fun setState(symbol: IrSymbol, newState: State) {
        memory[symbol]?.state = newState
    }

    fun getAll(): List<Pair<IrSymbol, Variable>> = memory.toList()
}

private class DataStack {
    private val stack = ArrayDeque<State>()

    fun push(state: State) {
        stack.add(state)
    }

    fun pop(): State = stack.removeLast()
    fun peek(): State? = stack.lastOrNull()
}