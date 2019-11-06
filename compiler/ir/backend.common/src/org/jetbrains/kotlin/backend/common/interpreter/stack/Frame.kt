/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.stack

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import java.util.NoSuchElementException

interface Frame {
    fun addVar(state: State)
    fun getVar(descriptor: DeclarationDescriptor): State
    fun getAll(): List<State>
}

class InterpreterFrame(val pool: MutableList<State> = mutableListOf()) : Frame {
    override fun addVar(state: State) {
        pool.add(state)
    }

    override fun getVar(descriptor: DeclarationDescriptor): State {
        return pool.firstOrNull { it.getDescriptor() == descriptor }
            ?: throw NoSuchElementException("Frame pool doesn't contains variable with descriptor $descriptor")
    }

    override fun getAll(): List<State> {
        return pool
    }
}
