/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.stack

import org.jetbrains.kotlin.backend.common.interpreter.ExecutionResult
import org.jetbrains.kotlin.backend.common.interpreter.exceptions.InterpreterException
import org.jetbrains.kotlin.backend.common.interpreter.state.State
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fileEntry
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

interface Stack {
    suspend fun newFrame(
        asSubFrame: Boolean = false, initPool: List<Variable> = listOf(), block: suspend () -> ExecutionResult
    ): ExecutionResult

    fun setCurrentFrameName(irFunction: IrFunction)
    fun getStackTrace(): List<String>

    fun clean()
    fun addVar(variable: Variable)
    fun addAll(variables: List<Variable>)
    fun getVariableState(variableDescriptor: DeclarationDescriptor): State
    fun getAll(): List<Variable>
    fun getAllTypeArguments(): List<Variable>

    fun contains(descriptor: DeclarationDescriptor): Boolean
    fun hasReturnValue(): Boolean
    fun pushReturnValue(state: State)
    fun popReturnValue(): State
    fun peekReturnValue(): State
}

class StackImpl : Stack {
    private val frameList = mutableListOf(FrameContainer()) // first frame is default, it is easier to work when last() is not null
    private fun getCurrentFrame() = frameList.last()

    override suspend fun newFrame(asSubFrame: Boolean, initPool: List<Variable>, block: suspend () -> ExecutionResult): ExecutionResult {
        val typeArgumentsPool = initPool.filter { it.descriptor is TypeParameterDescriptor }
        val valueArguments = initPool.filter { it.descriptor !is TypeParameterDescriptor }
        val newFrame = InterpreterFrame(valueArguments.toMutableList(), typeArgumentsPool)
        if (asSubFrame) getCurrentFrame().addSubFrame(newFrame) else frameList.add(FrameContainer(newFrame))

        return try {
            block()
        } finally {
            if (asSubFrame) getCurrentFrame().removeSubFrame() else removeLastFrame()
        }
    }

    private fun removeLastFrame() {
        if (frameList.size > 1 && getCurrentFrame().hasReturnValue()) frameList[frameList.lastIndex - 1].pushReturnValue(getCurrentFrame())
        frameList.removeAt(frameList.lastIndex)
    }

    override fun setCurrentFrameName(irFunction: IrFunction) {
        val fileName = irFunction.file.name
        val fileNameCapitalized = fileName.replace(".kt", "Kt").capitalize()
        val lineNum = irFunction.fileEntry.getLineNumber(irFunction.startOffset) + 1
        if (getCurrentFrame().frameEntryPoint == null)
            getCurrentFrame().frameEntryPoint = "at $fileNameCapitalized.${irFunction.fqNameWhenAvailable}($fileName:$lineNum)"
    }

    override fun getStackTrace(): List<String> {
        // TODO implement some sort of cache
        return frameList.mapNotNull { it.frameEntryPoint }
    }

    override fun clean() {
        frameList.clear()
        frameList.add(FrameContainer())
    }

    override fun addVar(variable: Variable) {
        getCurrentFrame().addVar(variable)
    }

    override fun addAll(variables: List<Variable>) {
        getCurrentFrame().addAll(variables)
    }

    override fun getVariableState(variableDescriptor: DeclarationDescriptor): State {
        return getCurrentFrame().getVariableState(variableDescriptor)
    }

    override fun getAll(): List<Variable> {
        return getCurrentFrame().getAll()
    }

    override fun getAllTypeArguments(): List<Variable> {
        return getCurrentFrame().getAllTypeArguments()
    }

    override fun contains(descriptor: DeclarationDescriptor): Boolean {
        return getCurrentFrame().contains(descriptor)
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
    var frameEntryPoint: String? = null
    private val innerStack = mutableListOf(current)
    private fun getTopFrame() = innerStack.first()

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
    fun getAllTypeArguments() = innerStack.flatMap { it.getAllTypeArguments() }
    fun getVariableState(variableDescriptor: DeclarationDescriptor): State {
        return innerStack.firstNotNullResult { it.tryGetVariableState(variableDescriptor) }
            ?: throw InterpreterException("$variableDescriptor not found") // TODO better message
    }

    fun contains(descriptor: DeclarationDescriptor) = innerStack.any { it.contains(descriptor) }
    fun hasReturnValue() = getTopFrame().hasReturnValue()
    fun pushReturnValue(container: FrameContainer) = getTopFrame().pushReturnValue(container.getTopFrame())
    fun pushReturnValue(state: State) = getTopFrame().pushReturnValue(state)
    fun popReturnValue() = getTopFrame().popReturnValue()
    fun peekReturnValue() = getTopFrame().peekReturnValue()

    override fun toString() = frameEntryPoint ?: "Not defined"
}