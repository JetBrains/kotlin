/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.common

import org.jetbrains.kotlin.codegen.inline.insnText
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.Value

abstract class FastAnalyzer<V : Value, I : Interpreter<V>, F : Frame<V>>(
    protected val owner: String,
    protected val method: MethodNode,
    protected val interpreter: I,
) {
    protected val nInsns = method.instructions.size()
    protected val handlers: Array<MutableList<TryCatchBlockNode>?> = arrayOfNulls(nInsns)

    private val frames: Array<Frame<V>?> = arrayOfNulls(nInsns)

    private val queued = BooleanArray(nInsns)
    private val queue = IntArray(nInsns)
    private var top = 0

    fun analyze(): Array<F?> {
        if (nInsns == 0) return getFrames()

        checkAssertions()
        computeExceptionHandlers(method)
        beforeAnalyze()

        analyzeMainLoop()

        return getFrames()
    }

    private fun analyzeMainLoop() {
        val current = newFrame(method.maxLocals, method.maxStack)
        val handler = newFrame(method.maxLocals, method.maxStack)
        initLocals(current)
        mergeControlFlowEdge(0, current)

        while (top > 0) {
            val insn = queue[--top]

            @Suppress("UNCHECKED_CAST")
            val f = frames[insn]!! as F
            queued[insn] = false

            val insnNode = method.instructions[insn]
            val insnOpcode = insnNode.opcode
            val insnType = insnNode.toType

            try {
                privateAnalyze(insnNode, insn, insnType, insnOpcode, f, current, handler)
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
    }

    open fun beforeAnalyze() {}

    abstract fun initLocals(current: F)

    abstract fun mergeControlFlowEdge(dest: Int, frame: F, canReuse: Boolean = false)

    abstract fun privateAnalyze(
        insnNode: AbstractInsnNode,
        insnIndex: Int,
        insnType: Int,
        insnOpcode: Int,
        currentlyAnalyzing: F,
        current: F,
        handler: F
    )

    protected abstract fun newFrame(nLocals: Int, nStack: Int): F

    @Suppress("UNCHECKED_CAST")
    fun getFrame(insn: AbstractInsnNode): F? = frames[insn.indexOf()] as? F

    @Suppress("UNCHECKED_CAST")
    protected fun getFrame(index: Int): F? = frames[index] as? F

    protected fun setFrame(index: Int, newFrame: F) {
        frames[index] = newFrame
    }

    @Suppress("UNCHECKED_CAST")
    private fun getFrames(): Array<F?> = frames as Array<F?>

    protected fun visitMeaningfulInstruction(insnNode: AbstractInsnNode, insnType: Int, insnOpcode: Int, current: F, insn: Int) {
        when {
            insnType == AbstractInsnNode.JUMP_INSN ->
                visitJumpInsnNode(insnNode as JumpInsnNode, current, insn, insnOpcode)
            insnType == AbstractInsnNode.LOOKUPSWITCH_INSN ->
                visitLookupSwitchInsnNode(insnNode as LookupSwitchInsnNode, current)
            insnType == AbstractInsnNode.TABLESWITCH_INSN ->
                visitTableSwitchInsnNode(insnNode as TableSwitchInsnNode, current)
            insnOpcode != Opcodes.ATHROW && (insnOpcode < Opcodes.IRETURN || insnOpcode > Opcodes.RETURN) ->
                visitOpInsn(insnNode, current, insn)
            else -> {
            }
        }
    }

    protected abstract fun visitJumpInsnNode(insnNode: JumpInsnNode, current: F, insn: Int, insnOpcode: Int)
    protected abstract fun visitLookupSwitchInsnNode(insnNode: LookupSwitchInsnNode, current: F)
    protected abstract fun visitTableSwitchInsnNode(insnNode: TableSwitchInsnNode, current: F)
    protected abstract fun visitOpInsn(insnNode: AbstractInsnNode, current: F, insn: Int)

    private fun checkAssertions() {
        if (method.instructions.any { it.opcode == Opcodes.JSR || it.opcode == Opcodes.RET })
            throw AssertionError("Subroutines are deprecated since Java 6")
    }

    protected fun AbstractInsnNode.indexOf() =
        method.instructions.indexOf(this)

    protected open fun useFastComputeExceptionHandlers(): Boolean = false

    private fun computeExceptionHandlers(m: MethodNode) {
        for (tcb in m.tryCatchBlocks) {
            if (useFastComputeExceptionHandlers()) computeExceptionHandlerFast(tcb) else computeExceptionHandlersForEachInsn(tcb)
        }
    }

    private fun computeExceptionHandlersForEachInsn(tcb: TryCatchBlockNode) {
        var current: AbstractInsnNode = tcb.start
        val end = tcb.end

        while (current != end) {
            if (current.isMeaningful) {
                val currentIndex = current.indexOf()
                var insnHandlers: MutableList<TryCatchBlockNode>? = handlers[currentIndex]
                if (insnHandlers == null) {
                    insnHandlers = SmartList()
                    handlers[currentIndex] = insnHandlers
                }
                insnHandlers.add(tcb)
            }
            current = current.next
        }
    }

    private fun computeExceptionHandlerFast(tcb: TryCatchBlockNode) {
        val start = tcb.start.indexOf()
        var insnHandlers: MutableList<TryCatchBlockNode>? = handlers[start]
        if (insnHandlers == null) {
            insnHandlers = ArrayList()
            handlers[start] = insnHandlers
        }
        insnHandlers.add(tcb)
    }

    protected fun updateQueue(changes: Boolean, dest: Int) {
        if (changes && !queued[dest]) {
            queued[dest] = true
            queue[top++] = dest
        }
    }

    protected fun F.dump(): String {
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
}