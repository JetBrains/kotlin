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

import org.jetbrains.kotlin.codegen.inline.insnOpcodeText
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
 * @see org.jetbrains.kotlin.codegen.optimization.fixStack.HackedFixStackMethodAnalyzerBase
 */
@Suppress("DuplicatedCode")
open class FastMethodAnalyzer<V : Value>(
    private val owner: String,
    val method: MethodNode,
    private val interpreter: Interpreter<V>
) {
    private val insnsArray = method.instructions.toArray()
    private val nInsns = method.instructions.size()

    // Single Predecessor Block (SPB) is a continuous sequence of instructions { I1, ... In } such that
    //  if I=insns[i] and J=insns[i+1] both belong to SPB,
    //  then I is a single immediate predecessor of J in a complete method control flow graph
    //  (including exception edges).
    //
    // Note that classic basic blocks are SPBs, but the opposite is not true:
    // SPBs have single entry point, but can have multiple exit points
    // (which lead to instructions not belonging to the given SPB).
    // Example:
    //      aload 1
    //      dup
    //      ifnull LA
    //      invokevirtual foo()
    //      dup
    //      ifnull LB
    //      invokevirtual bar()
    //      goto LC
    // is SPB (but not a basic block).
    //
    // For each J=insns[i+1] such that I=insns[i] belongs to the same SPB,
    // data flow transfer function
    //      Execute( J, Merge( { Out(K) | K <- Pred(J) } ) )
    // is effectively
    //      Execute( J, Out(I) ) )
    // so, we don't need to merge frames for such I->J edges.
    private val singlePredBlock = IntArray(nInsns)

    val frames: Array<Frame<V>?> = arrayOfNulls(nInsns)

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

        initSinglePredBlocks()

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
                } else {
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
                        else -> {
                        }
                    }
                }

                handlers[insn]?.forEach { tcb ->
                    val exnType = Type.getObjectType(tcb.type ?: "java/lang/Throwable")
                    val jump = tcb.handler.indexOf()

                    handler.init(f)
                    handler.clearStack()
                    handler.push(interpreter.newValue(exnType))
                    mergeControlFlowEdge(insn, jump, handler)
                }

            } catch (e: AnalyzerException) {
                throw AnalyzerException(e.node, "Error at instruction #$insn ${insnNode.insnText}: ${e.message}", e)
            } catch (e: Exception) {
                throw AnalyzerException(insnNode, "Error at instruction #$insn ${insnNode.insnText}: ${e.message}", e)
            }

        }

        return frames
    }

    private fun AbstractInsnNode.indexOf() =
        method.instructions.indexOf(this)

    private fun initSinglePredBlocks() {
        markSinglePredBlockEntries()
        markSinglePredBlockBodies()
    }

    private fun markSinglePredBlockEntries() {
        // Method entry point is SPB entry point.
        var blockId = 0
        singlePredBlock[0] = ++blockId

        // Every jump target is SPB entry point.
        for (insn in insnsArray) {
            when (insn) {
                is JumpInsnNode -> {
                    val labelIndex = insn.label.indexOf()
                    if (singlePredBlock[labelIndex] == 0) {
                        singlePredBlock[labelIndex] = ++blockId
                    }
                }
                is LookupSwitchInsnNode -> {
                    insn.dflt?.let { dfltLabel ->
                        val dfltIndex = dfltLabel.indexOf()
                        if (singlePredBlock[dfltIndex] == 0) {
                            singlePredBlock[dfltIndex] = ++blockId
                        }
                    }
                    for (label in insn.labels) {
                        val labelIndex = label.indexOf()
                        if (singlePredBlock[labelIndex] == 0) {
                            singlePredBlock[labelIndex] = ++blockId
                        }
                    }
                }
                is TableSwitchInsnNode -> {
                    insn.dflt?.let { dfltLabel ->
                        val dfltIndex = dfltLabel.indexOf()
                        if (singlePredBlock[dfltIndex] == 0) {
                            singlePredBlock[dfltIndex] = ++blockId
                        }
                    }
                    for (label in insn.labels) {
                        val labelIndex = label.indexOf()
                        if (singlePredBlock[labelIndex] == 0) {
                            singlePredBlock[labelIndex] = ++blockId
                        }
                    }
                }
            }
        }

        // Every try-catch block handler entry point is SPB entry point
        for (tcb in method.tryCatchBlocks) {
            val handlerIndex = tcb.handler.indexOf()
            if (singlePredBlock[handlerIndex] == 0) {
                singlePredBlock[handlerIndex] = ++blockId
            }
        }
    }

    private fun markSinglePredBlockBodies() {
        var current = 0
        for ((i, insn) in insnsArray.withIndex()) {
            if (singlePredBlock[i] == 0) {
                singlePredBlock[i] = current
            } else {
                // Entered a new SPB.
                current = singlePredBlock[i]
            }

            // GOTO, ATHROW, *RETURN instructions terminate current SPB.
            when (insn.opcode) {
                Opcodes.GOTO,
                Opcodes.ATHROW,
                in Opcodes.IRETURN..Opcodes.RETURN ->
                    current = 0
            }
        }
    }

    fun getFrame(insn: AbstractInsnNode): Frame<V>? =
        frames[insn.indexOf()]

    private fun checkAssertions() {
        if (insnsArray.any { it.opcode == Opcodes.JSR || it.opcode == Opcodes.RET })
            throw AssertionError("Subroutines are deprecated since Java 6")
    }

    private fun visitOpInsn(current: Frame<V>, insn: Int) {
        mergeControlFlowEdge(insn, insn + 1, current)
    }

    private fun visitTableSwitchInsnNode(insnNode: TableSwitchInsnNode, current: Frame<V>, insn: Int) {
        var jump = insnNode.dflt.indexOf()
        mergeControlFlowEdge(insn, jump, current)
        // In most cases order of visiting switch labels should not matter
        // The only one is a tableswitch being added in the beginning of coroutine method, these switch' labels may lead
        // in the middle of try/catch block, and FixStackAnalyzer is not ready for this (trying to restore stack before it was saved)
        // So we just fix the order of labels being traversed: the first one should be one at the method beginning
        // Using 'reversed' is because nodes are processed in LIFO order
        for (label in insnNode.labels.reversed()) {
            jump = label.indexOf()
            mergeControlFlowEdge(insn, jump, current)
        }
    }

    private fun visitLookupSwitchInsnNode(insnNode: LookupSwitchInsnNode, current: Frame<V>, insn: Int) {
        var jump = insnNode.dflt.indexOf()
        mergeControlFlowEdge(insn, jump, current)
        for (label in insnNode.labels) {
            jump = label.indexOf()
            mergeControlFlowEdge(insn, jump, current)
        }
    }

    private fun visitJumpInsnNode(insnNode: JumpInsnNode, current: Frame<V>, insn: Int, insnOpcode: Int) {
        if (insnOpcode != Opcodes.GOTO) {
            mergeControlFlowEdge(insn, insn + 1, current)
        }
        val jump = insnNode.label.indexOf()
        mergeControlFlowEdge(insn, jump, current)
    }

    private fun visitNopInsn(f: Frame<V>, insn: Int) {
        mergeControlFlowEdge(insn, insn + 1, f)
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
        mergeControlFlowEdge(0, 0, current)
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

    private fun mergeControlFlowEdge(src: Int, dest: Int, frame: Frame<V>) {
        val oldFrame = frames[dest]
        val changes = when {
            oldFrame == null -> {
                frames[dest] = newFrame(frame.locals, frame.maxStackSize).apply { init(frame) }
                true
            }
            dest == src + 1 && singlePredBlock[src] == singlePredBlock[dest] -> {
                // Forward jump within a single predecessor block, no need to merge.
                oldFrame.init(frame)
                true
            }
            else ->
                oldFrame.merge(frame, interpreter)
        }
        if (changes && !queued[dest]) {
            queued[dest] = true
            queue[top++] = dest
        }
    }

    @Suppress("unused")
    private fun dumpBlocksInfo() {
        fun LabelNode?.labelText() =
            if (this != null) "L#${indexOf()}" else "L<null>"

        println("===== ${method.name} ${method.signature} ======")
        for ((i, insn) in insnsArray.withIndex()) {
            val insnText = when (insn) {
                is LabelNode ->
                    "L#$i"
                is JumpInsnNode ->
                    "${insn.insnOpcodeText} ${insn.label.labelText()}"
                is TableSwitchInsnNode ->
                    "${insn.insnOpcodeText} min=${insn.min} max=${insn.max} \n\t\t\t" +
                            "[${insn.labels.joinToString { it.labelText() }}] \n\t\t\t" +
                            "dflt:${insn.dflt.labelText()}"
                is LookupSwitchInsnNode ->
                    "${insn.insnOpcodeText} \n\t\t\t" +
                            "[${insn.keys.zip(insn.labels).joinToString { (key, label) -> "$key: ${label.labelText()}"}}] \n\t\t\t" +
                            "dflt:${insn.dflt.labelText()}"
                else ->
                    insn.insnText
            }
            println("$i\t${singlePredBlock[i]}\t$insnText")
        }
        for (tcb in method.tryCatchBlocks) {
            println("\tTCB start:${tcb.start.labelText()} end:${tcb.end.labelText()} handler:${tcb.handler.labelText()}")
        }
        println()
    }
}
