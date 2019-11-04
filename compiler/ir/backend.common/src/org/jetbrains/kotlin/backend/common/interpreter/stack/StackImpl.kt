/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.stack

import java.util.NoSuchElementException

class StackImpl : Stack {
    private val stack: MutableList<Frame> = mutableListOf()

    override fun pushFrame(frame: Frame) {
        stack.add(frame)
    }

    override fun popFrame(): Frame {
        if (stack.isEmpty()) {
            throw IndexOutOfBoundsException("Stack is empty")
        }
        return stack.removeAt(stack.size - 1)
    }

    override fun peekFrame(): Frame {
        if (stack.isEmpty()) {
            throw IndexOutOfBoundsException("Stack is empty")
        }
        return stack.get(stack.size - 1)
    }
}

class InterpreterFrame : Frame {
    val pool: MutableList<State> = mutableListOf()

    override fun addVar(state: State) {
        pool.add(state)
    }

    override fun getVar(name: String): State {
        return pool.firstOrNull { it.getName() == name }
            ?: throw NoSuchElementException("Frame pool doesn't contains variable with name $name")
    }
}
