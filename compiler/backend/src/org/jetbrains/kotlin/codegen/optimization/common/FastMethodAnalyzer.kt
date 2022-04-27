/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
*/

package org.jetbrains.kotlin.codegen.optimization.common

import org.jetbrains.kotlin.codegen.inline.insnText
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.Value

/**
 * @see org.jetbrains.kotlin.codegen.optimization.fixStack.FastStackAnalyzer
 */
@Suppress("DuplicatedCode")
open class FastMethodAnalyzer<V : Value>
@JvmOverloads constructor(
    private val owner: String,
    private val method: MethodNode,
    private val interpreter: Interpreter<V>,
    private val pruneExceptionEdges: Boolean = false
) {
    private val insnsArray = method.instructions.toArray()
    private val nInsns = method.instructions.size()

    private val isMergeNode = findMergeNodes(method)

    private val frames: Array<Frame<V>?> = arrayOfNulls(nInsns)

    private val handlers: Array<MutableList<TryCatchBlockNode>?> = arrayOfNulls(nInsns)
    private val queued = BooleanArray(nInsns)
    private val queue = IntArray(nInsns)
    private var top = 0

    protected open fun newFrame(nLocals: Int, nStack: Int): Frame<V> =
        Frame(nLocals, nStack)

    fun analyze(): Array<Frame<V>?> {
        if (nInsns == 0) return frames

        checkAssertions()
        computeExceptionHandlersForEachInsn(method)

        val isTcbStart = BooleanArray(nInsns)
        for (tcb in method.tryCatchBlocks) {
            isTcbStart[tcb.start.indexOf() + 1] = true
        }

        val current = newFrame(method.maxLocals, method.maxStack)
        val handler = newFrame(method.maxLocals, method.maxStack)
        initLocals(current)
        mergeControlFlowEdge(0, current)

        while (top > 0) {
            val insn = queue[--top]
            val f = frames[insn]!!
            queued[insn] = false

            val insnNode = method.instructions[insn]
            try {
                val insnOpcode = insnNode.opcode
                val insnType = insnNode.type

                if (insnType == AbstractInsnNode.LABEL ||
                    insnType == AbstractInsnNode.LINE ||
                    insnType == AbstractInsnNode.FRAME ||
                    insnOpcode == Opcodes.NOP
                ) {
                    mergeControlFlowEdge(insn + 1, f, canReuse = true)
                } else {
                    current.init(f).execute(insnNode, interpreter)
                    when {
                        insnType == AbstractInsnNode.JUMP_INSN ->
                            visitJumpInsnNode(insnNode as JumpInsnNode, current, insn, insnOpcode)
                        insnType == AbstractInsnNode.LOOKUPSWITCH_INSN ->
                            visitLookupSwitchInsnNode(insnNode as LookupSwitchInsnNode, current)
                        insnType == AbstractInsnNode.TABLESWITCH_INSN ->
                            visitTableSwitchInsnNode(insnNode as TableSwitchInsnNode, current)
                        insnOpcode != Opcodes.ATHROW && (insnOpcode < Opcodes.IRETURN || insnOpcode > Opcodes.RETURN) ->
                            visitOpInsn(current, insn)
                        else -> {
                        }
                    }
                }

                // Jump by an exception edge clears the stack, putting exception on top.
                // So, unless we have a store operation, anything we change on stack would be lost,
                // and there's no need to analyze exception handler again.
                // Add an exception edge from TCB start to make sure handler itself is still visited.
                if (!pruneExceptionEdges ||
                    insnOpcode in Opcodes.ISTORE..Opcodes.ASTORE ||
                    insnOpcode == Opcodes.IINC ||
                    isTcbStart[insn]
                ) {
                    handlers[insn]?.forEach { tcb ->
                        val exnType = Type.getObjectType(tcb.type ?: "java/lang/Throwable")
                        val jump = tcb.handler.indexOf()

                        handler.init(f)
                        handler.clearStack()
                        handler.push(interpreter.newExceptionValue(tcb, handler, exnType))
                        mergeControlFlowEdge(jump, handler)
                    }
                }

            } catch (e: AnalyzerException) {
                throw AnalyzerException(
                    e.node,
                    "Error at instruction #$insn ${insnNode.insnText(method.instructions)}: ${e.message}\ncurrent: ${current.dump()}",
                    e
                )
            } catch (e: Exception) {
                throw AnalyzerException(
                    insnNode,
                    "Error at instruction #$insn ${insnNode.insnText(method.instructions)}: ${e.message}\ncurrent: ${current.dump()}",
                    e
                )
            }

        }

        return frames
    }

    internal fun initLocals(current: Frame<V>) {
        current.setReturn(interpreter.newReturnTypeValue(Type.getReturnType(method.desc)))
        val args = Type.getArgumentTypes(method.desc)
        var local = 0
        val isInstanceMethod = (method.access and Opcodes.ACC_STATIC) == 0
        if (isInstanceMethod) {
            current.setLocal(local, interpreter.newParameterValue(true, local, Type.getObjectType(owner)))
            local++
        }
        for (arg in args) {
            current.setLocal(local, interpreter.newParameterValue(isInstanceMethod, local, arg))
            local++
            if (arg.size == 2) {
                current.setLocal(local, interpreter.newEmptyValue(local))
                local++
            }
        }
        while (local < method.maxLocals) {
            current.setLocal(local, interpreter.newEmptyValue(local))
            local++
        }
    }

    private fun AbstractInsnNode.indexOf() =
        method.instructions.indexOf(this)

    fun getFrame(insn: AbstractInsnNode): Frame<V>? =
        frames[insn.indexOf()]

    private fun checkAssertions() {
        if (insnsArray.any { it.opcode == Opcodes.JSR || it.opcode == Opcodes.RET })
            throw AssertionError("Subroutines are deprecated since Java 6")
    }

    private fun visitOpInsn(current: Frame<V>, insn: Int) {
        mergeControlFlowEdge(insn + 1, current)
    }

    private fun visitTableSwitchInsnNode(insnNode: TableSwitchInsnNode, current: Frame<V>) {
        mergeControlFlowEdge(insnNode.dflt.indexOf(), current)
        // In most cases order of visiting switch labels should not matter
        // The only one is a tableswitch being added in the beginning of coroutine method, these switch' labels may lead
        // in the middle of try/catch block, and FixStackAnalyzer is not ready for this (trying to restore stack before it was saved)
        // So we just fix the order of labels being traversed: the first one should be one at the method beginning
        // Using 'reversed' is because nodes are processed in LIFO order
        for (label in insnNode.labels.asReversed()) {
            mergeControlFlowEdge(label.indexOf(), current)
        }
    }

    private fun visitLookupSwitchInsnNode(insnNode: LookupSwitchInsnNode, current: Frame<V>) {
        mergeControlFlowEdge(insnNode.dflt.indexOf(), current)
        for (label in insnNode.labels) {
            mergeControlFlowEdge(label.indexOf(), current)
        }
    }

    private fun visitJumpInsnNode(insnNode: JumpInsnNode, current: Frame<V>, insn: Int, insnOpcode: Int) {
        mergeControlFlowEdge(insnNode.label.indexOf(), current)
        if (insnOpcode != Opcodes.GOTO) {
            mergeControlFlowEdge(insn + 1, current)
        }
    }

    private fun computeExceptionHandlersForEachInsn(m: MethodNode) {
        for (tcb in m.tryCatchBlocks) {
            val begin = tcb.start.indexOf()
            val end = tcb.end.indexOf()
            for (j in begin until end) {
                if (!insnsArray[j].isMeaningful) continue
                var insnHandlers: MutableList<TryCatchBlockNode>? = handlers[j]
                if (insnHandlers == null) {
                    insnHandlers = SmartList()
                    handlers[j] = insnHandlers
                }
                insnHandlers.add(tcb)
            }
        }
    }

    private fun mergeControlFlowEdge(dest: Int, frame: Frame<V>, canReuse: Boolean = false) {
        val oldFrame = frames[dest]
        val changes = when {
            canReuse && !isMergeNode[dest] -> {
                frames[dest] = frame
                true
            }
            oldFrame == null -> {
                frames[dest] = newFrame(frame.locals, frame.maxStackSize).apply { init(frame) }
                true
            }
            !isMergeNode[dest] -> {
                oldFrame.init(frame)
                true
            }
            else ->
                try {
                    oldFrame.merge(frame, interpreter)
                } catch (e: AnalyzerException) {
                    throw AnalyzerException(null, "${e.message}\nframe: ${frame.dump()}\noldFrame: ${oldFrame.dump()}")
                }
        }
        if (changes && !queued[dest]) {
            queued[dest] = true
            queue[top++] = dest
        }
    }

    private fun Frame<V>.dump(): String {
        return buildString {
            append("{\n")
            append("  locals: [\n")
            for (i in 0 until method.maxLocals) {
                append("    #$i: ${this@dump.getLocal(i)}\n")
            }
            append("  ]\n")
            val stackSize = this@dump.stackSize
            append("  stack: size=")
            append(stackSize)
            if (stackSize == 0) {
                append(" []\n")
            } else {
                append(" [\n")
                for (i in 0 until stackSize) {
                    append("    #$i: ${this@dump.getStack(i)}\n")
                }
                append("  ]\n")
            }
            append("}\n")
        }
    }

    companion object {
        fun findMergeNodes(method: MethodNode): BooleanArray {
            val isMergeNode = BooleanArray(method.instructions.size())
            for (insn in method.instructions) {
                when (insn.type) {
                    AbstractInsnNode.JUMP_INSN -> {
                        val jumpInsn = insn as JumpInsnNode
                        isMergeNode[method.instructions.indexOf(jumpInsn.label)] = true
                    }
                    AbstractInsnNode.LOOKUPSWITCH_INSN -> {
                        val switchInsn = insn as LookupSwitchInsnNode
                        isMergeNode[method.instructions.indexOf(switchInsn.dflt)] = true
                        for (label in switchInsn.labels) {
                            isMergeNode[method.instructions.indexOf(label)] = true
                        }
                    }
                    AbstractInsnNode.TABLESWITCH_INSN -> {
                        val switchInsn = insn as TableSwitchInsnNode
                        isMergeNode[method.instructions.indexOf(switchInsn.dflt)] = true
                        for (label in switchInsn.labels) {
                            isMergeNode[method.instructions.indexOf(label)] = true
                        }
                    }
                }
            }
            for (tcb in method.tryCatchBlocks) {
                isMergeNode[method.instructions.indexOf(tcb.handler)] = true
            }
            return isMergeNode
        }
    }
}
