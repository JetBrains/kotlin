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

package org.jetbrains.kotlin.codegen.optimization.temporaryVals

import org.jetbrains.kotlin.codegen.optimization.common.FastAnalyzer
import org.jetbrains.kotlin.codegen.optimization.common.toType
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Opcodes.API_VERSION
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.Value

interface StoreLoadValue : Value

abstract class StoreLoadInterpreter<V : StoreLoadValue> : Interpreter<V>(API_VERSION) {
    abstract fun iinc(insn: IincInsnNode, value: V): V
}

class StoreLoadFrame<V : StoreLoadValue>(val maxLocals: Int) : Frame<V>(maxLocals, 0) {
    private val locals = arrayOfNulls<StoreLoadValue>(maxLocals)

    @Suppress("UNCHECKED_CAST")
    operator fun get(index: Int): V =
        locals[index] as V

    override fun setLocal(index: Int, value: V) {
        locals[index] = value
    }

    fun init(other: StoreLoadFrame<V>): StoreLoadFrame<V> {
        System.arraycopy(other.locals, 0, this.locals, 0, locals.size)
        return this
    }

    fun execute(insn: AbstractInsnNode, interpreter: StoreLoadInterpreter<V>) {
        when (insn.opcode) {
            in Opcodes.ISTORE..Opcodes.ASTORE -> {
                val varInsn = insn as VarInsnNode
                locals[varInsn.`var`] = interpreter.copyOperation(varInsn, null)
            }
            in Opcodes.ILOAD..Opcodes.ALOAD -> {
                val varInsn = insn as VarInsnNode
                interpreter.copyOperation(varInsn, this[varInsn.`var`])
            }
            Opcodes.IINC -> {
                val iincInsn = insn as IincInsnNode
                interpreter.iinc(iincInsn, this[iincInsn.`var`])
            }
        }
    }

    fun merge(other: StoreLoadFrame<V>, interpreter: StoreLoadInterpreter<V>): Boolean {
        var changes = false
        for (i in locals.indices) {
            val oldValue = this[i]
            val newValue = interpreter.merge(oldValue, other[i])
            if (newValue != oldValue) {
                changes = true
                this.setLocal(i, newValue)
            }
        }
        return changes
    }
}

class FastStoreLoadAnalyzer<V : StoreLoadValue>(
    owner: String,
    method: MethodNode,
    interpreter: StoreLoadInterpreter<V>
) : FastAnalyzer<V, StoreLoadInterpreter<V>, StoreLoadFrame<V>>(owner, method, interpreter) {
    private val isMergeNode = BooleanArray(nInsns)

    override fun beforeAnalyze() {
        initMergeNodes()
    }

    override fun analyzeInstruction(
        insnNode: AbstractInsnNode,
        insnIndex: Int,
        insnType: Int,
        insnOpcode: Int,
        currentlyAnalyzing: StoreLoadFrame<V>,
        current: StoreLoadFrame<V>,
        handler: StoreLoadFrame<V>,
    ) {
        if (insnType == AbstractInsnNode.LABEL || insnType == AbstractInsnNode.LINE || insnType == AbstractInsnNode.FRAME) {
            mergeControlFlowEdge(insnIndex + 1, currentlyAnalyzing)
        } else {
            current.init(currentlyAnalyzing).execute(insnNode, interpreter)
            visitMeaningfulInstruction(insnNode, insnType, insnOpcode, current, insnIndex)
        }

        handlers[insnIndex]?.forEach { tcb ->
            val jump = tcb.handler.indexOf()
            handler.init(currentlyAnalyzing)
            mergeControlFlowEdge(jump, handler)
        }
    }

    override fun newFrame(nLocals: Int, nStack: Int): StoreLoadFrame<V> = StoreLoadFrame<V>(nLocals)

    override fun visitOpInsn(insnNode: AbstractInsnNode, current: StoreLoadFrame<V>, insn: Int) {
        mergeControlFlowEdge(insn + 1, current)
    }

    override fun visitTableSwitchInsnNode(insnNode: TableSwitchInsnNode, current: StoreLoadFrame<V>) {
        mergeControlFlowEdge(insnNode.dflt.indexOf(), current)
        for (label in insnNode.labels) {
            mergeControlFlowEdge(label.indexOf(), current)
        }
    }

    override fun visitLookupSwitchInsnNode(insnNode: LookupSwitchInsnNode, current: StoreLoadFrame<V>) {
        mergeControlFlowEdge(insnNode.dflt.indexOf(), current)
        for (label in insnNode.labels) {
            mergeControlFlowEdge(label.indexOf(), current)
        }
    }

    override fun visitJumpInsnNode(insnNode: JumpInsnNode, current: StoreLoadFrame<V>, insn: Int, insnOpcode: Int) {
        if (insnOpcode != Opcodes.GOTO) {
            mergeControlFlowEdge(insn + 1, current)
        }
        mergeControlFlowEdge(insnNode.label.indexOf(), current)
    }

    private fun initMergeNodes() {
        for (insn in method.instructions) {
            when (insn.toType) {
                AbstractInsnNode.JUMP_INSN -> {
                    val jumpInsn = insn as JumpInsnNode
                    isMergeNode[jumpInsn.label.indexOf()] = true
                }
                AbstractInsnNode.LOOKUPSWITCH_INSN -> {
                    val switchInsn = insn as LookupSwitchInsnNode
                    isMergeNode[switchInsn.dflt.indexOf()] = true
                    for (label in switchInsn.labels) {
                        isMergeNode[label.indexOf()] = true
                    }
                }
                AbstractInsnNode.TABLESWITCH_INSN -> {
                    val switchInsn = insn as TableSwitchInsnNode
                    isMergeNode[switchInsn.dflt.indexOf()] = true
                    for (label in switchInsn.labels) {
                        isMergeNode[label.indexOf()] = true
                    }
                }
            }
        }
        for (tcb in method.tryCatchBlocks) {
            isMergeNode[tcb.handler.indexOf()] = true
        }
    }

    override fun initLocals(current: StoreLoadFrame<V>) {
        val args = Type.getArgumentTypes(method.desc)
        var local = 0
        val isInstanceMethod = (method.access and Opcodes.ACC_STATIC) == 0
        if (isInstanceMethod) {
            val ctype = Type.getObjectType(owner)
            current.setLocal(local, interpreter.newValue(ctype))
            local++
        }
        for (arg in args) {
            current.setLocal(local, interpreter.newValue(arg))
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

    override fun mergeControlFlowEdge(dest: Int, frame: StoreLoadFrame<V>, canReuse: Boolean) {
        val oldFrame = getFrame(dest)
        val changes = when {
            oldFrame == null -> {
                setFrame(dest, newFrame(frame.maxLocals, 0).init(frame))
                true
            }
            !isMergeNode[dest] -> {
                oldFrame.init(frame)
                true
            }
            else ->
                oldFrame.merge(frame, interpreter)
        }
        updateQueue(changes, dest)
    }
}