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

package org.jetbrains.kotlin.codegen.optimization.fixStack

import org.jetbrains.kotlin.codegen.optimization.common.FastAnalyzer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.Value

/**
 * @see org.jetbrains.kotlin.codegen.optimization.common.FastMethodAnalyzer
 */
// This is a very specific version of method bytecode analyzer that doesn't perform any DFA,
// but infers stack types for reachable instructions instead.
internal open class FastStackAnalyzer<V : Value, F : Frame<V>>(
    owner: String,
    method: MethodNode,
    interpreter: Interpreter<V>
) : FastAnalyzer<V, Interpreter<V>, F>(owner, method, interpreter) {
    @Suppress("UNCHECKED_CAST")
    override fun newFrame(nLocals: Int, nStack: Int): F = Frame<V>(nLocals, nStack) as F

    protected open fun visitControlFlowEdge(insnNode: AbstractInsnNode, successor: Int): Boolean = true

    protected open fun visitControlFlowExceptionEdge(insn: Int, successor: Int): Boolean = true

    // Don't have to visit the same exception handler multiple times - we care only about stack state at TCB start.
    override fun useFastComputeExceptionHandlers(): Boolean = true

    override fun analyzeInstruction(
        insnNode: AbstractInsnNode,
        insnIndex: Int,
        insnType: Int,
        insnOpcode: Int,
        currentlyAnalyzing: F,
        current: F,
        handler: F,
    ) {
        if (insnType == AbstractInsnNode.LABEL || insnType == AbstractInsnNode.LINE || insnType == AbstractInsnNode.FRAME) {
            visitNopInsn(insnNode, currentlyAnalyzing, insnIndex)
        } else {
            current.init(currentlyAnalyzing)
            if (insnOpcode != Opcodes.RETURN) {
                // Don't care about possibly incompatible return type
                current.execute(insnNode, interpreter)
            }
            visitMeaningfulInstruction(insnNode, insnType, insnOpcode, current, insnIndex)
        }

        handlers[insnIndex]?.forEach { tcb ->
            val exnType = Type.getObjectType(tcb.type ?: "java/lang/Throwable")
            val jump = tcb.handler.indexOf()
            if (visitControlFlowExceptionEdge(insnIndex, jump)) {
                handler.init(currentlyAnalyzing)
                handler.clearStack()
                handler.push(interpreter.newValue(exnType))
                mergeControlFlowEdge(jump, handler)
            }
        }
    }

    override fun visitOpInsn(insnNode: AbstractInsnNode, current: F, insn: Int) {
        processControlFlowEdge(current, insnNode, insn + 1)
    }

    private fun visitNopInsn(insnNode: AbstractInsnNode, f: F, insn: Int) {
        processControlFlowEdge(f, insnNode, insn + 1)
    }

    override fun visitTableSwitchInsnNode(insnNode: TableSwitchInsnNode, current: F) {
        var jump = insnNode.dflt.indexOf()
        processControlFlowEdge(current, insnNode, jump)
        // In most cases order of visiting switch labels should not matter
        // The only one is a tableswitch being added in the beginning of coroutine method, these switch' labels may lead
        // in the middle of try/catch block, and FixStackAnalyzer is not ready for this (trying to restore stack before it was saved)
        // So we just fix the order of labels being traversed: the first one should be one at the method beginning
        // Using 'reversed' is because nodes are processed in LIFO order
        for (label in insnNode.labels.reversed()) {
            jump = label.indexOf()
            processControlFlowEdge(current, insnNode, jump)
        }
    }

    override fun visitLookupSwitchInsnNode(insnNode: LookupSwitchInsnNode, current: F) {
        var jump = insnNode.dflt.indexOf()
        processControlFlowEdge(current, insnNode, jump)
        for (label in insnNode.labels) {
            jump = label.indexOf()
            processControlFlowEdge(current, insnNode, jump)
        }
    }

    override fun visitJumpInsnNode(insnNode: JumpInsnNode, current: F, insn: Int, insnOpcode: Int) {
        if (insnOpcode != Opcodes.GOTO) {
            processControlFlowEdge(current, insnNode, insn + 1)
        }
        val jump = insnNode.label.indexOf()
        processControlFlowEdge(current, insnNode, jump)
    }

    private fun processControlFlowEdge(current: F, insnNode: AbstractInsnNode, jump: Int) {
        if (visitControlFlowEdge(insnNode, jump)) {
            mergeControlFlowEdge(jump, current)
        }
    }

    override fun mergeControlFlowEdge(dest: Int, frame: F, canReuse: Boolean) {
        val oldFrame = getFrame(dest)
        val changes = when {
            // Don't have to visit the same instruction multiple times - we care only about "initial" stack state.
            oldFrame == null -> {
                setFrame(dest, newFrame(frame.locals, frame.maxStackSize).apply { init(frame) })
                true
            }
            else -> false
        }
        updateQueue(changes, dest)
    }

}
