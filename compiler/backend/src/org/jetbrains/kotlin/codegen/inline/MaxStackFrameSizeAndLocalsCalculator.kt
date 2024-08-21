/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.utils.SmartIdentityTable
import org.jetbrains.org.objectweb.asm.*
import java.util.*
import kotlin.math.max

/**
 * This class is based on `org.objectweb.asm.MethodWriter`
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 * @author Denis Zharkov
 */
class MaxStackFrameSizeAndLocalsCalculator(api: Int, access: Int, descriptor: String?, mv: MethodVisitor?) :
    MaxLocalsCalculator(api, access, descriptor, mv) {
    private val labelWrappersTable = SmartIdentityTable<Label, LabelWrapper>()
    private val exceptionHandlers: MutableCollection<ExceptionHandler> = LinkedList()

    private val firstLabel: LabelWrapper

    /**
     * The (relative) stack size after the last visited instruction. This size
     * is relative to the beginning of the current basic block, i.e., the true
     * stack size after the last visited instruction is equal to the
     * [MaxStackFrameSizeAndLocalsCalculator.LabelWrapper.inputStackSize] of the current basic block
     * plus <tt>stackSize</tt>.
     */
    private var stackSize = 0

    /**
     * The (relative) maximum stack size after the last visited instruction.
     * This size is relative to the beginning of the current basic block, i.e.,
     * the true maximum stack size after the last visited instruction is equal
     * to the [MaxStackFrameSizeAndLocalsCalculator.LabelWrapper.inputStackSize] of the current basic
     * block plus <tt>stackSize</tt>.
     */
    private var maxStackSize = 0

    /**
     * Maximum stack size of this method.
     */
    private var maxStack = 0

    private var currentBlock: LabelWrapper? = null
    private var previousBlock: LabelWrapper? = null

    init {
        firstLabel = getLabelWrapper(Label())
        processLabel(firstLabel.label)
    }

    override fun visitFrame(type: Int, nLocal: Int, local: Array<Any>, nStack: Int, stack: Array<Any>) {
        throw AssertionError("We don't support visitFrame because currently nobody needs")
    }

    override fun visitInsn(opcode: Int) {
        increaseStackSize(FRAME_SIZE_CHANGE_BY_OPCODE[opcode])
        // if opcode == ATHROW or xRETURN, ends current block (no successor)
        if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
            noSuccessor()
        }
        super.visitInsn(opcode)
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        // BIPUSH and SIPUSH add a value, NEWARRAY does not
        if (opcode != Opcodes.NEWARRAY) {
            increaseStackSize(1)
        }
        super.visitIntInsn(opcode, operand)
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        increaseStackSize(FRAME_SIZE_CHANGE_BY_OPCODE[opcode])
        super.visitVarInsn(opcode, `var`)
    }

    override fun visitTypeInsn(opcode: Int, type: String) {
        // ANEWARRAY, CHECKCASH, and INSTANCEOF don't change the stack
        if (opcode == Opcodes.NEW) {
            increaseStackSize(1)
        }
        super.visitTypeInsn(opcode, type)
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) {
        val c = desc[0]
        val stackSizeVariation = when (opcode) {
            Opcodes.GETSTATIC -> if (c == 'D' || c == 'J') 2 else 1
            Opcodes.PUTSTATIC -> if (c == 'D' || c == 'J') -2 else -1
            Opcodes.GETFIELD -> if (c == 'D' || c == 'J') 1 else 0
            else /* Opcodes.PUTFIELD */ -> if (c == 'D' || c == 'J') -3 else -2
        }
        increaseStackSize(stackSizeVariation)
        super.visitFieldInsn(opcode, owner, name, desc)
    }

    override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
        val argSize = Type.getArgumentsAndReturnSizes(desc)
        increaseStackSize((argSize and 0x03) - (argSize shr 2) + (if (opcode == Opcodes.INVOKESTATIC) 1 else 0))
        super.visitMethodInsn(opcode, owner, name, desc, itf)
    }

    override fun visitInvokeDynamicInsn(name: String, desc: String, bsm: Handle, vararg bsmArgs: Any) {
        val argSize = Type.getArgumentsAndReturnSizes(desc)
        increaseStackSize((argSize and 0x03) - (argSize shr 2) + 1)
        super.visitInvokeDynamicInsn(name, desc, bsm, *bsmArgs)
    }

    override fun visitJumpInsn(opcode: Int, label: Label) {
        if (currentBlock != null) {
            // This is always negative so no need to update the maximum
            stackSize += FRAME_SIZE_CHANGE_BY_OPCODE[opcode]
            addSuccessor(getLabelWrapper(label), stackSize)
            if (opcode == Opcodes.GOTO) {
                noSuccessor()
            }
        }
        super.visitJumpInsn(opcode, label)
    }

    override fun visitLabel(label: Label) {
        processLabel(label)
        super.visitLabel(label)
    }

    private fun processLabel(label: Label) {
        val wrapper = getLabelWrapper(label)

        if (currentBlock != null) {
            // ends current block (with one new successor)
            currentBlock!!.outputStackMax = maxStackSize
            addSuccessor(wrapper, stackSize)
        }

        currentBlock = wrapper
        stackSize = 0
        maxStackSize = 0
        previousBlock?.nextLabel = wrapper
        previousBlock = wrapper
    }

    override fun visitLdcInsn(cst: Any?) {
        increaseStackSize(if (cst is Long || cst is Double) 2 else 1)
        super.visitLdcInsn(cst)
    }

    override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label, vararg labels: Label) {
        visitSwitchInsn(dflt, labels)
        super.visitTableSwitchInsn(min, max, dflt, *labels)
    }

    override fun visitLookupSwitchInsn(dflt: Label, keys: IntArray, labels: Array<out Label>) {
        visitSwitchInsn(dflt, labels)
        super.visitLookupSwitchInsn(dflt, keys, labels)
    }

    private fun visitSwitchInsn(dflt: Label, labels: Array<out Label>) {
        if (currentBlock != null) {
            --stackSize
            addSuccessor(getLabelWrapper(dflt), stackSize)
            for (label in labels) {
                addSuccessor(getLabelWrapper(label), stackSize)
            }
            noSuccessor()
        }
    }

    override fun visitMultiANewArrayInsn(desc: String, dims: Int) {
        if (currentBlock != null) {
            increaseStackSize(dims - 1)
        }

        super.visitMultiANewArrayInsn(desc, dims)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        for (handler in exceptionHandlers) {
            var l: LabelWrapper? = handler.start
            val e = handler.end

            while (l !== e) {
                checkNotNull(l) { "Bad exception handler end" }

                l.addSuccessor(handler.handlerLabel, 0, true)
                l = l.nextLabel
            }
        }

        /*
         * control flow analysis algorithm: while the block stack is not
         * empty, pop a block from this stack, update the max stack size,
         * compute the true (non relative) begin stack size of the
         * successors of this block, and push these successors onto the
         * stack (unless they have already been pushed onto the stack).
         * Note: by hypothesis, the {@link LabelWrapper#inputStackSize} of the
         * blocks in the block stack are the true (non relative) beginning
         * stack sizes of these blocks.
         */
        var max = 0
        val stack = Stack<LabelWrapper>()
        val pushed: MutableSet<LabelWrapper> = HashSet()

        stack.push(firstLabel)
        pushed.add(firstLabel)

        while (!stack.empty()) {
            val current = stack.pop()
            val start = current.inputStackSize

            val blockMax = start + current.outputStackMax
            if (blockMax > max) {
                max = blockMax
            }

            for (edge in current.successors) {
                val successor = edge.successor
                if (!pushed.contains(successor)) {
                    successor.inputStackSize = if (edge.isExceptional) 1 else start + edge.outputStackSize
                    pushed.add(successor)
                    stack.push(successor)
                }
            }
        }

        this.maxStack = max(this.maxStack, max(maxStack, max))
        super.visitMaxs(this.maxStack, maxLocals)
    }

    override fun visitTryCatchBlock(start: Label, end: Label, handler: Label, type: String?) {
        exceptionHandlers.add(ExceptionHandler(getLabelWrapper(start), getLabelWrapper(end), getLabelWrapper(handler)))
        super.visitTryCatchBlock(start, end, handler, type)
    }

    private class ExceptionHandler(val start: LabelWrapper, val end: LabelWrapper, val handlerLabel: LabelWrapper)

    private class ControlFlowEdge(val successor: LabelWrapper, val outputStackSize: Int, val isExceptional: Boolean)

    private class LabelWrapper(val label: Label, private val index: Int) {
        var nextLabel: LabelWrapper? = null
        val successors: MutableCollection<ControlFlowEdge> = LinkedList()

        var outputStackMax: Int = 0
        var inputStackSize: Int = 0

        fun addSuccessor(successor: LabelWrapper, outputStackSize: Int, isExceptional: Boolean) {
            successors.add(ControlFlowEdge(successor, outputStackSize, isExceptional))
        }

        override fun equals(other: Any?): Boolean = other is LabelWrapper && other.index == index
        override fun hashCode(): Int = index
    }

    // ------------------------------------------------------------------------
    // Utility methods
    // ------------------------------------------------------------------------
    private fun getLabelWrapper(label: Label): LabelWrapper {
        return labelWrappersTable.getOrCreate(label) { LabelWrapper(label, labelWrappersTable.size) }
    }

    private fun increaseStackSize(variation: Int) {
        val size = stackSize + variation
        if (size > maxStackSize) {
            maxStackSize = size
        }
        stackSize = size
    }

    private fun addSuccessor(successor: LabelWrapper, outputStackSize: Int) {
        currentBlock!!.addSuccessor(successor, outputStackSize, false)
    }

    /**
     * Ends the current basic block. This method must be used in the case where
     * the current basic block does not have any successor.
     */
    private fun noSuccessor() {
        if (currentBlock != null) {
            currentBlock!!.outputStackMax = maxStackSize
            currentBlock = null
        }
    }

    companion object {
        private val FRAME_SIZE_CHANGE_BY_OPCODE: IntArray

        init {
            // copy-pasted from org.jetbrains.org.objectweb.asm.Frame
            val s = ("EFFFFFFFFGGFFFGGFFFEEFGFGFEEEEEEEEEEEEEEEEEEEEDEDEDDDDD"
                    + "CDCDEEEEEEEEEEEEEEEEEEEEBABABBBBDCFFFGGGEDCDCDCDCDCDCDCDCD"
                    + "CDCEEEEDDDDDDDCDCDCEFEFDDEEFFDEDEEEBDDBBDDDDDDCCCCCCCCEFED"
                    + "DDCDCDEEEEEEEEEEFEEEEEEDDEEDDEE")
            FRAME_SIZE_CHANGE_BY_OPCODE = IntArray(s.length) { s[it].code - 'E'.code }
        }
    }
}
