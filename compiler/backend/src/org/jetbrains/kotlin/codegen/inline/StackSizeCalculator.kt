/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.codegen.optimization.fixStack.FastStackAnalyzer
import org.jetbrains.kotlin.codegen.optimization.fixStack.FixStackInterpreter
import org.jetbrains.kotlin.codegen.optimization.fixStack.FixStackValue
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter

internal class StackSizeCalculator(owner: String, method: MethodNode) :
    FastStackAnalyzer<FixStackValue>(owner, method, FixStackInterpreter()) {

    fun calculateStackSize(): Int {
        val frames = analyze()
        return frames.maxOf { frame ->
            if (frame is ExpandableStackFrame) frame.getActualStackSize() else method.maxStack
        }
    }

    override fun newFrame(nLocals: Int, nStack: Int): Frame<FixStackValue> =
        ExpandableStackFrame(nLocals, nStack)

    class ExpandableStackFrame(nLocals: Int, nStack: Int) : Frame<FixStackValue>(nLocals, nStack) {
        private val extraStack = Stack<FixStackValue>()

        override fun init(src: Frame<out FixStackValue>): Frame<FixStackValue> {
            extraStack.clear()
            extraStack.addAll((src as ExpandableStackFrame).extraStack)
            return super.init(src)
        }

        override fun clearStack() {
            extraStack.clear()
            super.clearStack()
        }

        override fun execute(insn: AbstractInsnNode, interpreter: Interpreter<FixStackValue>) {
            if (insn.opcode == Opcodes.RETURN) return
            super.execute(insn, interpreter)
        }

        override fun push(value: FixStackValue) {
            if (super.getStackSize() < maxStackSize) {
                super.push(value)
            } else {
                extraStack.add(value)
            }
        }

        override fun pop(): FixStackValue =
            if (extraStack.isNotEmpty()) {
                extraStack.pop()
            } else {
                super.pop()
            }

        override fun setStack(i: Int, value: FixStackValue) {
            if (i < maxStackSize) {
                super.setStack(i, value)
            } else {
                extraStack[i - maxStackSize] = value
            }
        }

        override fun merge(frame: Frame<out FixStackValue>, interpreter: Interpreter<FixStackValue>): Boolean {
            throw UnsupportedOperationException("Stack normalization should not merge frames")
        }

        fun getActualStackSize() = super.getStackSize() + extraStack.sumOf { it.size }
    }
}