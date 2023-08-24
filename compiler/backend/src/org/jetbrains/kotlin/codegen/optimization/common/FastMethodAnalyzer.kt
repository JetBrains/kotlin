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
class FastMethodAnalyzer<V : Value>
@JvmOverloads constructor(
    owner: String,
    method: MethodNode,
    interpreter: Interpreter<V>,
    private val pruneExceptionEdges: Boolean = false,
    private val createFrame: (Int, Int) -> Frame<V> = { nLocals, nStack -> Frame<V>(nLocals, nStack) }
) : FastAnalyzer<V, Interpreter<V>, Frame<V>>(owner, method, interpreter) {
    private val isMergeNode = findMergeNodes(method)
    private val isTcbStart = BooleanArray(nInsns)

    override fun newFrame(nLocals: Int, nStack: Int): Frame<V> = createFrame(nLocals, nStack)

    override fun beforeAnalyze() {
        for (tcb in method.tryCatchBlocks) {
            isTcbStart[tcb.start.indexOf() + 1] = true
        }
    }

    override fun analyzeInstruction(
        insnNode: AbstractInsnNode,
        insnIndex: Int,
        insnType: Int,
        insnOpcode: Int,
        currentlyAnalyzing: Frame<V>,
        current: Frame<V>,
        handler: Frame<V>,
    ) {
        if (insnType == AbstractInsnNode.LABEL ||
            insnType == AbstractInsnNode.LINE ||
            insnType == AbstractInsnNode.FRAME ||
            insnOpcode == Opcodes.NOP
        ) {
            mergeControlFlowEdge(insnIndex + 1, currentlyAnalyzing, canReuse = true)
        } else {
            current.init(currentlyAnalyzing).execute(insnNode, interpreter)
            visitMeaningfulInstruction(insnNode, insnType, insnOpcode, current, insnIndex)
        }

        // Jump by an exception edge clears the stack, putting exception on top.
        // So, unless we have a store operation, anything we change on stack would be lost,
        // and there's no need to analyze exception handler again.
        // Add an exception edge from TCB start to make sure handler itself is still visited.
        if (!pruneExceptionEdges ||
            insnOpcode in Opcodes.ISTORE..Opcodes.ASTORE ||
            insnOpcode == Opcodes.IINC ||
            isTcbStart[insnIndex]
        ) {
            handlers[insnIndex]?.forEach { tcb ->
                val exnType = Type.getObjectType(tcb.type ?: "java/lang/Throwable")
                val jump = tcb.handler.indexOf()

                handler.init(currentlyAnalyzing)
                handler.clearStack()
                handler.push(interpreter.newExceptionValue(tcb, handler, exnType))
                mergeControlFlowEdge(jump, handler)
            }
        }
    }

    override fun visitOpInsn(insnNode: AbstractInsnNode, current: Frame<V>, insn: Int) {
        mergeControlFlowEdge(insn + 1, current)
    }

    override fun visitTableSwitchInsnNode(insnNode: TableSwitchInsnNode, current: Frame<V>) {
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

    override fun visitLookupSwitchInsnNode(insnNode: LookupSwitchInsnNode, current: Frame<V>) {
        mergeControlFlowEdge(insnNode.dflt.indexOf(), current)
        for (label in insnNode.labels) {
            mergeControlFlowEdge(label.indexOf(), current)
        }
    }

    override fun visitJumpInsnNode(insnNode: JumpInsnNode, current: Frame<V>, insn: Int, insnOpcode: Int) {
        mergeControlFlowEdge(insnNode.label.indexOf(), current)
        if (insnOpcode != Opcodes.GOTO) {
            mergeControlFlowEdge(insn + 1, current)
        }
    }

    /**
     * Updates frame at the index [dest] with its old value if provided and previous control flow node frame [frame].
     * Reuses old frame when possible and when [canReuse] is true.
     * If updated, adds the frame to the queue
     */
    override fun mergeControlFlowEdge(dest: Int, frame: Frame<V>, canReuse: Boolean) {
        val oldFrame = getFrame(dest)
        val changes = when {
            canReuse && !isMergeNode[dest] -> {
                setFrame(dest, frame)
                true
            }
            oldFrame == null -> {
                setFrame(dest, newFrame(frame.locals, frame.maxStackSize).apply { init(frame) })
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
        updateQueue(changes, dest)
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
