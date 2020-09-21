/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.stack

import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.interpreter.ExecutionResult
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterError
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isLocal

internal interface Stack {
    fun newFrame(asSubFrame: Boolean = false, initPool: List<Variable> = listOf(), block: () -> ExecutionResult): ExecutionResult
    fun newFrame(irFunction: IrFunction, initPool: List<Variable> = listOf(), block: () -> ExecutionResult): ExecutionResult

    fun fixCallEntryPoint(irExpression: IrExpression)
    fun withEntryPoint(irFunction: IrFunction? = null, irFile: IrFile? = null, block: () -> ExecutionResult): ExecutionResult
    fun getStackTrace(): List<String>
    fun getStackCount(): Int

    fun clean()
    fun addVar(variable: Variable)
    fun addAll(variables: List<Variable>)
    fun getVariable(symbol: IrSymbol): Variable
    fun getAll(): List<Variable>

    fun contains(symbol: IrSymbol): Boolean
    fun hasReturnValue(): Boolean
    fun pushReturnValue(state: State)
    fun popReturnValue(): State
    fun peekReturnValue(): State
}

internal class StackImpl : Stack {
    private val frameList = mutableListOf(FrameContainer()) // first frame is default, it is easier to work when last() is not null
    private fun getCurrentFrame() = frameList.last()
    private var stackCount = 0

    override fun newFrame(asSubFrame: Boolean, initPool: List<Variable>, block: () -> ExecutionResult): ExecutionResult {
        val typeArgumentsPool = initPool.filter { it.symbol is IrTypeParameterSymbol }
        val valueArguments = initPool.filter { it.symbol !is IrTypeParameterSymbol }
        val newFrame = InterpreterFrame(valueArguments.toMutableList(), typeArgumentsPool)
        if (asSubFrame) getCurrentFrame().addSubFrame(newFrame) else frameList.add(FrameContainer(newFrame))

        return try {
            stackCount++
            block()
        } finally {
            stackCount--
            if (asSubFrame) getCurrentFrame().removeSubFrame() else removeLastFrame()
        }
    }

    override fun newFrame(irFunction: IrFunction, initPool: List<Variable>, block: () -> ExecutionResult): ExecutionResult {
        val asSubFrame = irFunction.isInline || irFunction.isLocal
        return newFrame(asSubFrame, initPool) {
            withEntryPoint(irFunction, irFunction.fileOrNull) {
                block()
            }
        }
    }

    private fun removeLastFrame() {
        if (frameList.size > 1 && getCurrentFrame().hasReturnValue()) frameList[frameList.lastIndex - 1].pushReturnValue(getCurrentFrame())
        frameList.removeAt(frameList.lastIndex)
    }

    override fun fixCallEntryPoint(irExpression: IrExpression) {
        val fileEntry = getCurrentFrame().entryPoint?.irFile?.fileEntry ?: return
        val lineNum = fileEntry.getLineNumber(irExpression.startOffset) + 1
        getCurrentFrame().entryPoint?.lineNumber = lineNum
    }

    // TODO remove method and create EntryPoint in frame constructor
    override fun withEntryPoint(irFunction: IrFunction?, irFile: IrFile?, block: () -> ExecutionResult): ExecutionResult {
        val frame = getCurrentFrame()
        if (frame.entryPoint == null && irFile != null) frame.entryPoint = FrameContainer.Companion.EntryPoint(irFunction, irFile)
        return block()
    }

    override fun getStackTrace(): List<String> {
        // TODO implement some sort of cache
        return frameList.map { it.toString() }
    }

    override fun getStackCount(): Int = stackCount

    override fun clean() {
        stackCount = 0
        frameList.clear()
        frameList.add(FrameContainer())
    }

    override fun addVar(variable: Variable) {
        getCurrentFrame().addVar(variable)
    }

    override fun addAll(variables: List<Variable>) {
        getCurrentFrame().addAll(variables)
    }

    override fun getVariable(symbol: IrSymbol): Variable {
        return getCurrentFrame().getVariable(symbol)
    }

    override fun getAll(): List<Variable> {
        return getCurrentFrame().getAll()
    }

    override fun contains(symbol: IrSymbol): Boolean {
        return getCurrentFrame().contains(symbol)
    }

    override fun hasReturnValue(): Boolean {
        return getCurrentFrame().hasReturnValue()
    }

    override fun pushReturnValue(state: State) {
        getCurrentFrame().pushReturnValue(state)
    }

    override fun popReturnValue(): State {
        return getCurrentFrame().popReturnValue()
    }

    override fun peekReturnValue(): State {
        return getCurrentFrame().peekReturnValue()
    }
}

private class FrameContainer(current: Frame = InterpreterFrame()) {
    var entryPoint: EntryPoint? = null
    private val innerStack = mutableListOf(current)
    private fun getTopFrame() = innerStack.first()

    companion object {
        class EntryPoint(val irFunction: IrFunction?, val irFile: IrFile) {
            var lineNumber: Int = -1
        }
    }

    fun addSubFrame(frame: Frame) {
        innerStack.add(0, frame)
    }

    fun removeSubFrame() {
        if (getTopFrame().hasReturnValue() && innerStack.size > 1) innerStack[1].pushReturnValue(getTopFrame())
        innerStack.removeAt(0)
    }

    fun addVar(variable: Variable) = getTopFrame().addVar(variable)
    fun addAll(variables: List<Variable>) = getTopFrame().addAll(variables)
    fun getAll() = innerStack.flatMap { it.getAll() }
    fun getVariable(symbol: IrSymbol): Variable {
        return innerStack.firstNotNullOfOrNull { it.getVariable(symbol) }
            ?: throw InterpreterError("$symbol not found") // TODO better message
    }

    fun contains(symbol: IrSymbol) = innerStack.any { it.contains(symbol) }
    fun hasReturnValue() = getTopFrame().hasReturnValue()
    fun pushReturnValue(container: FrameContainer) = getTopFrame().pushReturnValue(container.getTopFrame())
    fun pushReturnValue(state: State) = getTopFrame().pushReturnValue(state)
    fun popReturnValue() = getTopFrame().popReturnValue()
    fun peekReturnValue() = getTopFrame().peekReturnValue()

    override fun toString(): String {
        entryPoint?.irFile ?: return "Not defined"
        val fileNameCapitalized = entryPoint!!.irFile.name.replace(".kt", "Kt").capitalize()
        val lineNum = if (entryPoint?.lineNumber != -1) ":${entryPoint?.lineNumber}" else ""
        return "at $fileNameCapitalized.${entryPoint?.irFunction?.fqNameWhenAvailable ?: "<clinit>"}(${entryPoint!!.irFile.name}$lineNum)"
    }
}
