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

package org.jetbrains.kotlin.codegen.optimization.common

import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.Value
import java.util.*

/**
 * This class is a modified version of `org.objectweb.asm.tree.analysis.Analyzer`
 * @author Eric Bruneton
 * @author Dmitry Petrov
 */
open class MethodAnalyzer<V : Value>(
        val owner: String,
        val method: MethodNode,
        protected val interpreter: Interpreter<V>
) {
    val instructions: InsnList = method.instructions
    val nInsns: Int = instructions.size()

    val frames: Array<Frame<V>?> = arrayOfNulls(nInsns)

    private val handlers: Array<MutableList<TryCatchBlockNode>?> = arrayOfNulls(nInsns)
    private val queued: BooleanArray = BooleanArray(nInsns)
    private val queue: IntArray = IntArray(nInsns)
    private var top: Int = 0

    protected open fun init(owner: String, m: MethodNode) {}

    protected open fun newFrame(nLocals: Int, nStack: Int): Frame<V> = Frame(nLocals, nStack)

    protected open fun newFrame(src: Frame<out V>): Frame<V> {
        val frame = newFrame(src.locals, src.maxStackSize)
        frame.init(src)
        return frame
    }

    protected open fun visitControlFlowEdge(insn: Int, successor: Int): Boolean = true

    protected open fun visitControlFlowExceptionEdge(insn: Int, successor: Int): Boolean = true

    protected open fun visitControlFlowExceptionEdge(insn: Int, tcb: TryCatchBlockNode): Boolean =
            visitControlFlowExceptionEdge(insn, instructions.indexOf(tcb.handler))

    fun analyze(): Array<Frame<V>?> {
        if (nInsns == 0) return frames

        checkAssertions()

        computeExceptionHandlersForEachInsn(method)

        val current = newFrame(method.maxLocals, method.maxStack)
        val handler = newFrame(method.maxLocals, method.maxStack)
        initControlFlowAnalysis(current, method, owner)

        while (top > 0) {
            val insn = queue[--top]
            val f = frames[insn]!!
            queued[insn] = false

            val insnNode = method.instructions[insn]
            try {
                val insnOpcode = insnNode.opcode
                val insnType = insnNode.type

                if (insnType == AbstractInsnNode.LABEL || insnType == AbstractInsnNode.LINE || insnType == AbstractInsnNode.FRAME) {
                    visitNopInsn(f, insn)
                }
                else {
                    current.init(f).execute(insnNode, interpreter)

                    when {
                        insnNode is JumpInsnNode ->
                            visitJumpInsnNode(insnNode, current, insn, insnOpcode)
                        insnNode is LookupSwitchInsnNode ->
                            visitLookupSwitchInsnNode(insnNode, current, insn)
                        insnNode is TableSwitchInsnNode ->
                            visitTableSwitchInsnNode(insnNode, current, insn)
                        insnOpcode != Opcodes.ATHROW && (insnOpcode < Opcodes.IRETURN || insnOpcode > Opcodes.RETURN) ->
                            visitOpInsn(current, insn)
                        else -> {}
                    }
                }

                handlers[insn]?.forEach { tcb ->
                    val exnType = Type.getObjectType(tcb.type?:"java/lang/Throwable")
                    val jump = instructions.indexOf(tcb.handler)
                    if (visitControlFlowExceptionEdge(insn, tcb)) {
                        handler.init(f)
                        handler.clearStack()
                        handler.push(interpreter.newValue(exnType))
                        mergeControlFlowEdge(jump, handler)
                    }
                }

            }
            catch (e: AnalyzerException) {
                throw AnalyzerException(e.node, "Error at instruction #" + insn + " ${InlineCodegenUtil.getInsnText(insnNode)}: " + e.message, e)
            }
            catch (e: Exception) {
                throw AnalyzerException(insnNode, "Error at instruction #" + insn + " ${InlineCodegenUtil.getInsnText(insnNode)}: " + e.message, e)
            }

        }

        return frames
    }

    fun getFrame(insn: AbstractInsnNode): Frame<V>? =
            frames[instructions.indexOf(insn)]

    private fun checkAssertions() {
        if (instructions.toArray().any { it.opcode == Opcodes.JSR || it.opcode == Opcodes.RET })
            throw AssertionError("Subroutines are deprecated since Java 6")
    }

    private fun visitOpInsn(current: Frame<V>, insn: Int) {
        processControlFlowEdge(current, insn, insn + 1)
    }

    private fun visitTableSwitchInsnNode(insnNode: TableSwitchInsnNode, current: Frame<V>, insn: Int) {
        var jump = instructions.indexOf(insnNode.dflt)
        processControlFlowEdge(current, insn, jump)
        // In most cases order of visiting switch labels should not matter
        // The only one is a tableswitch being added in the beginning of coroutine method, these switch' labels may lead
        // in the middle of try/catch block, and FixStackAnalyzer is not ready for this (trying to restore stack before it was saved)
        // So we just fix the order of labels being traversed: the first one should be one at the method beginning
        // Using 'reversed' is because nodes are processed in LIFO order
        for (label in insnNode.labels.reversed()) {
            jump = instructions.indexOf(label)
            processControlFlowEdge(current, insn, jump)
        }
    }

    private fun visitLookupSwitchInsnNode(insnNode: LookupSwitchInsnNode, current: Frame<V>, insn: Int) {
        var jump = instructions.indexOf(insnNode.dflt)
        processControlFlowEdge(current, insn, jump)
        for (label in insnNode.labels) {
            jump = instructions.indexOf(label)
            processControlFlowEdge(current, insn, jump)
        }
    }

    private fun visitJumpInsnNode(insnNode: JumpInsnNode, current: Frame<V>, insn: Int, insnOpcode: Int) {
        if (insnOpcode != Opcodes.GOTO && insnOpcode != Opcodes.JSR) {
            processControlFlowEdge(current, insn, insn + 1)
        }
        val jump = instructions.indexOf(insnNode.label)
        processControlFlowEdge(current, insn, jump)
    }

    private fun visitNopInsn(f: Frame<V>, insn: Int) {
        processControlFlowEdge(f, insn, insn + 1)
    }

    private fun processControlFlowEdge(current: Frame<V>, insn: Int, jump: Int) {
        if (visitControlFlowEdge(insn, jump)) {
            mergeControlFlowEdge(jump, current)
        }
    }

    private fun initControlFlowAnalysis(current: Frame<V>, m: MethodNode, owner: String) {
        current.setReturn(interpreter.newValue(Type.getReturnType(m.desc)))
        val args = Type.getArgumentTypes(m.desc)
        var local = 0
        if ((m.access and Opcodes.ACC_STATIC) == 0) {
            val ctype = Type.getObjectType(owner)
            current.setLocal(local++, interpreter.newValue(ctype))
        }
        for (arg in args) {
            current.setLocal(local++, interpreter.newValue(arg))
            if (arg.size == 2) {
                current.setLocal(local++, interpreter.newValue(null))
            }
        }
        while (local < m.maxLocals) {
            current.setLocal(local++, interpreter.newValue(null))
        }
        mergeControlFlowEdge(0, current)

        init(owner, m)
    }

    private fun computeExceptionHandlersForEachInsn(m: MethodNode) {
        for (tcb in m.tryCatchBlocks) {
            val begin = instructions.indexOf(tcb.start)
            val end = instructions.indexOf(tcb.end)
            for (j in begin..end - 1) {
                var insnHandlers: MutableList<TryCatchBlockNode>? = handlers[j]
                if (insnHandlers == null) {
                    insnHandlers = ArrayList<TryCatchBlockNode>()
                    handlers[j] = insnHandlers
                }
                insnHandlers.add(tcb)
            }
        }
    }

    private fun mergeControlFlowEdge(insn: Int, frame: Frame<V>) {
        val oldFrame = frames[insn]
        val changes: Boolean

        if (oldFrame == null) {
            frames[insn] = newFrame(frame)
            changes = true
        }
        else {
            changes = oldFrame.merge(frame, interpreter)
        }
        if (changes && !queued[insn]) {
            queued[insn] = true
            queue[top++] = insn
        }
    }

}
