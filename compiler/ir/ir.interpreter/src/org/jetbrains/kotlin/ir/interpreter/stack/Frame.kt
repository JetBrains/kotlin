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
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

internal class Frame(subFrame: SubFrame, val irFile: IrFile? = null) {
    private val innerStack = mutableListOf(subFrame)
    private var currentInstruction: Instruction? = null
    val currentSubFrameOwner: IrElement
        get() = getCurrentFrame().owner

    companion object {
        const val NOT_DEFINED = "Not defined"
    }

    private fun getCurrentFrame() = innerStack.last()

    fun addSubFrame(frame: SubFrame) {
        innerStack.add(frame)
    }

    fun removeSubFrame() {
        getCurrentFrame().peekState()?.let { if (innerStack.size > 1) innerStack[innerStack.size - 2].pushState(it) }
        removeSubFrameWithoutDataPropagation()
    }

    fun removeSubFrameWithoutDataPropagation() {
        innerStack.removeLast()
    }

    fun hasNoSubFrames() = innerStack.isEmpty()
    fun hasNoInstructions() = hasNoSubFrames() || (innerStack.size == 1 && innerStack.first().isEmpty())

    fun addInstruction(instruction: Instruction) {
        getCurrentFrame().pushInstruction(instruction)
    }

    fun popInstruction(): Instruction {
        return getCurrentFrame().popInstruction().apply { currentInstruction = this }
    }

    fun dropInstructions() = getCurrentFrame().dropInstructions()

    fun pushState(state: State) {
        getCurrentFrame().pushState(state)
    }

    fun popState(): State = getCurrentFrame().popState()
    fun peekState(): State? = getCurrentFrame().peekState()

    fun addVariable(variable: Variable) {
        getCurrentFrame().addVariable(variable)
    }

    fun getVariable(symbol: IrSymbol): Variable {
        return innerStack.firstNotNullResult { it.getVariable(symbol) }
            ?: throw InterpreterError("$symbol not found") // TODO better message
    }

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

internal class SubFrame(private val instructions: MutableList<Instruction>, val owner: IrElement) {
    private val memory = mutableListOf<Variable>()
    private val dataStack = DataStack()

    fun isEmpty() = instructions.isEmpty()

    fun pushInstruction(instruction: Instruction) {
        instructions.add(0, instruction)
    }

    fun popInstruction(): Instruction {
        return instructions.removeFirst()
    }

    fun dropInstructions() = instructions.lastOrNull()?.apply { instructions.clear() }

    fun pushState(state: State) {
        dataStack.push(state)
    }

    fun popState(): State = dataStack.pop()
    fun peekState(): State? = if (!dataStack.isEmpty()) dataStack.peek() else null

    fun addVariable(variable: Variable) {
        memory += variable
    }

    fun getVariable(symbol: IrSymbol): Variable? = memory.firstOrNull { it.symbol == symbol }
    fun getAll(): List<Variable> = memory
}

private class DataStack {
    private val stack = mutableListOf<State>()

    fun isEmpty() = stack.isEmpty()

    fun push(state: State) {
        stack.add(state)
    }

    fun pop(): State = stack.removeLast()
    fun peek(): State = stack.last()
}