/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.stack

import org.jetbrains.kotlin.backend.common.interpreter.equalTo
import org.jetbrains.kotlin.backend.common.interpreter.state.State
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import kotlin.NoSuchElementException

interface Frame {
    fun addVar(variable: Variable)
    fun addAll(variables: List<Variable>)
    fun getVariableState(variableDescriptor: DeclarationDescriptor): State
    fun tryGetVariableState(variableDescriptor: DeclarationDescriptor): State?
    fun getAll(): List<Variable>
    fun getAllTypeArguments(): List<Variable> // TODO try to get rid of this method; possibly by finding all type arguments in local class
    fun contains(descriptor: DeclarationDescriptor): Boolean
    fun pushReturnValue(state: State)
    fun pushReturnValue(frame: Frame) // TODO rename to getReturnValueFrom
    fun peekReturnValue(): State
    fun popReturnValue(): State
    fun hasReturnValue(): Boolean
}

// TODO replace exceptions with InterpreterException
class InterpreterFrame(
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

    override fun tryGetVariableState(variableDescriptor: DeclarationDescriptor): State? {
        return (if (variableDescriptor is TypeParameterDescriptor) typeArguments else pool)
            .firstOrNull { it.descriptor.equalTo(variableDescriptor) }?.state
    }

    override fun getVariableState(variableDescriptor: DeclarationDescriptor): State {
        return tryGetVariableState(variableDescriptor)
            ?: throw NoSuchElementException("Frame pool doesn't contains variable with descriptor $variableDescriptor")
    }

    override fun getAll(): List<Variable> {
        return pool
    }

    override fun getAllTypeArguments(): List<Variable> {
        return typeArguments
    }

    override fun contains(descriptor: DeclarationDescriptor): Boolean {
        return (typeArguments + pool).any { it.descriptor == descriptor }
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
        throw NoSuchElementException("Return values stack is empty")
    }

    override fun popReturnValue(): State {
        if (returnStack.isNotEmpty()) {
            val item = returnStack.last()
            returnStack.removeAt(returnStack.size - 1)
            return item
        }
        throw NoSuchElementException("Return values stack is empty")
    }
}
