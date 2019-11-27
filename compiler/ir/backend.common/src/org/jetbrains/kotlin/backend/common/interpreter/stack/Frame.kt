/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.stack

import org.jetbrains.kotlin.backend.common.interpreter.equalTo
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import java.util.*

data class Variable(val descriptor: DeclarationDescriptor, val state: State) {
    override fun toString(): String {
        val descriptorName = when (descriptor) {
            is ReceiverParameterDescriptor -> descriptor.containingDeclaration.name.toString() + "::this"
            else -> descriptor.name
        }
        return "Variable(descriptor=$descriptorName, state=$state)"
    }
}

interface Frame {
    fun addVar(variable: Variable)
    fun addAll(variables: List<Variable>)
    fun getVariableState(variableDescriptor: DeclarationDescriptor): State
    fun getAll(): List<Variable>
    fun contains(descriptor: DeclarationDescriptor): Boolean
}

class InterpreterFrame(val pool: MutableList<Variable> = mutableListOf()) : Frame {
    override fun addVar(variable: Variable) {
        pool.add(variable)
    }

    override fun addAll(variables: List<Variable>) {
        pool.addAll(variables)
    }

    override fun getVariableState(variableDescriptor: DeclarationDescriptor): State {
        return pool.firstOrNull { it.descriptor.equalTo(variableDescriptor) }?.state
            ?: throw NoSuchElementException("Frame pool doesn't contains variable with descriptor $variableDescriptor")
    }

    override fun getAll(): List<Variable> {
        return pool
    }

    override fun contains(descriptor: DeclarationDescriptor): Boolean {
        return pool.any { it.descriptor == descriptor }
    }
}
