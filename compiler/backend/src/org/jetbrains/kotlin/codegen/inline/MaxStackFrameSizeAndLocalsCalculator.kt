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
import org.jetbrains.kotlin.utils.SmartList
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

    private val basicBlocks = SmartIdentityTable<Label, BasicBlock>()
    private val stack = Stack<BasicBlock>()

    private var currentBlockStackDelta = 0
    private var currentBlock: BasicBlock? = BasicBlock().apply { fixInputStackSize(0) }

    // Occasionally TransformationMethodVisitor re-invokes visitMaxs (it assumes 0 could mean an invalid
    // stack size, which may or may not be false), so cache the result
    private var result = 0

    override fun visitFrame(type: Int, nLocal: Int, local: Array<Any>, nStack: Int, stack: Array<Any>) {
        throw AssertionError("We don't support visitFrame because currently nobody needs")
    }

    override fun visitInsn(opcode: Int) {
        increaseStackSize(FRAME_SIZE_CHANGE_BY_OPCODE[opcode])
        if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
            currentBlock = null
        }
        super.visitInsn(opcode)
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        if (opcode != Opcodes.NEWARRAY) { // BIPUSH or SIPUSH
            increaseStackSize(1)
        }
        super.visitIntInsn(opcode, operand)
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        increaseStackSize(FRAME_SIZE_CHANGE_BY_OPCODE[opcode])
        super.visitVarInsn(opcode, `var`)
    }

    override fun visitTypeInsn(opcode: Int, type: String) {
        if (opcode == Opcodes.NEW) {
            increaseStackSize(1)
        } // else ANEWARRAY, CHECKCAST, or INSTANCEOF
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
        // This is always negative so no need to update the maximum
        currentBlock?.let {
            currentBlockStackDelta += FRAME_SIZE_CHANGE_BY_OPCODE[opcode]
            it.addSuccessor(label)
            if (opcode == Opcodes.GOTO) {
                currentBlock = null
            }
        }
        super.visitJumpInsn(opcode, label)
    }

    override fun visitLabel(label: Label) {
        currentBlock?.addSuccessor(label)
        currentBlock = getBasicBlockAt(label)
        currentBlockStackDelta = 0
        super.visitLabel(label)
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
        currentBlock?.let {
            --currentBlockStackDelta
            it.addSuccessor(dflt)
            for (label in labels) {
                it.addSuccessor(label)
            }
        }
        currentBlock = null
    }

    override fun visitMultiANewArrayInsn(desc: String, dims: Int) {
        increaseStackSize(dims - 1)
        super.visitMultiANewArrayInsn(desc, dims)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        var max = result
        // Put normal path before exception handlers (this makes the failsafe below trigger less)
        stack.reverse()
        // Blocks should be entered with a consistent stack state, otherwise the frame map
        // will not be valid. This means we can use a simple DFS/BFS because the moment
        // we reach a block through any path we know the size of the stack for all paths.
        while (!stack.empty()) {
            val current = stack.pop()
            val start = current.inputStackSize
            max = max(max, start + current.maxStackDelta)
            for ((successor, deltaAtJump) in current.successors) {
                // Fix-stack pseudoinstructions can sometimes cause this to go into the negative.
                // In this case we can just treat that as 0, as the fix-stack transformer
                // will reserve the extra stack space unconditionally.
                successor.fixInputStackSize(max(0, start + deltaAtJump))
            }
        }
        result = max
        super.visitMaxs(max(maxStack, max), maxLocals)
    }

    override fun visitTryCatchBlock(start: Label, end: Label, handler: Label, type: String?) {
        getBasicBlockAt(handler).fixInputStackSize(1)
        super.visitTryCatchBlock(start, end, handler, type)
    }

    private class BasicBlock {
        val successors: MutableList<Pair<BasicBlock, Int>> = SmartList()
        var inputStackSize: Int = -1 // negative means unknown because no predecessors have been processed yet
        var maxStackDelta: Int = 0
    }

    private fun getBasicBlockAt(label: Label): BasicBlock {
        return basicBlocks.getOrCreate(label) { BasicBlock() }
    }

    private fun increaseStackSize(variation: Int) {
        currentBlock?.let {
            val size = currentBlockStackDelta + variation
            if (variation > 0 && it.maxStackDelta < size) {
                it.maxStackDelta = size
            }
            currentBlockStackDelta = size
        }
    }

    private fun BasicBlock.addSuccessor(label: Label) {
        successors.add(getBasicBlockAt(label) to currentBlockStackDelta)
    }

    private fun BasicBlock.fixInputStackSize(value: Int) {
        if (inputStackSize < 0) {
            inputStackSize = value
            stack.add(this)
        } // else assert(inputStackSize == value)
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
