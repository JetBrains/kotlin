/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.stack

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import java.util.NoSuchElementException

interface Frame {
    fun addVar(state: State)
    fun addAll(states: List<State>)
    fun getVar(descriptor: DeclarationDescriptor): State
    fun getAll(): List<State>
    fun contains(descriptor: DeclarationDescriptor): Boolean
}

class InterpreterFrame(val pool: MutableList<State> = mutableListOf()) : Frame {
    override fun addVar(state: State) {
        pool.add(state)
    }

    override fun addAll(states: List<State>) {
        pool.addAll(states)
    }

    override fun getVar(descriptor: DeclarationDescriptor): State {
        return pool.firstOrNull { it.isTypeOf(descriptor) }
            ?: throw NoSuchElementException("Frame pool doesn't contains variable with descriptor $descriptor")
    }

    override fun getAll(): List<State> {
        return pool
    }

    override fun contains(descriptor: DeclarationDescriptor): Boolean {
        return pool.any { it.isTypeOf(descriptor) }
    }
}
