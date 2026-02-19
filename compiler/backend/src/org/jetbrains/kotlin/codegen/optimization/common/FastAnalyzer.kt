/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

abstract class FastAnalyzer<V : Value, F : Frame<V>>(
    private val owner: String,
    private val method: MethodNode,
    private val interpreter: Interpreter<V>,
    private val pruneExceptionEdges: Boolean,
    private val useFastComputeExceptionHandlers: Boolean,
    private val useFastMergeControlFlowEdge: Boolean,
    private val newFrame: (Int, Int) -> F
) {
    private val nInsns = method.instructions.size()
    private val frames: Array<Frame<V>?> = arrayOfNulls(nInsns)

    private val handlers: Array<MutableList<TryCatchBlockNode>?> = arrayOfNulls(nInsns)
    private val isTcbStart = BooleanArray(nInsns)
    private val isMergeNode = findMergeNodes(method)

    private val queued = BooleanArray(nInsns)
    private val queue = IntArray(nInsns)
    private var top = 0

    @Suppress("UNCHECKED_CAST")
    fun getFrame(insn: AbstractInsnNode): F? = frames[insn.indexOf()] as? F

    fun analyze(): Array<Frame<V>?> {
        if (nInsns == 0) return frames

        checkAssertions()
        computeExceptionHandlers(method)

        for (tcb in method.tryCatchBlocks) {
            isTcbStart[tcb.start.indexOf() + 1] = true
        }

        beforeAnalyze()

        analyzeMainLoop()

        return frames
    }

    private fun analyzeMainLoop() {
        val current = newFrame(method.maxLocals, method.maxStack)
        val handler = newFrame(method.maxLocals, method.maxStack)
        initLocals(current)
        mergeControlFlowEdge(0, current)

        while (top > 0) {
            val insn = queue[--top]

            @Suppress("UNCHECKED_CAST")
            val f = frames[insn] as F
            queued[insn] = false

            val insnNode = method.instructions[insn]

            try {
                analyzeInstruction(insnNode, insn, f, current, handler)
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

    private fun analyzeInstruction(
        insnNode: AbstractInsnNode,
        insnIndex: Int,
        currentlyAnalyzing: F,
        current: F,
        handler: F
    ) {
        val insnOpcode = insnNode.opcode
        val insnType = insnNode.nodeType

        if (insnType == AbstractInsnNode.LABEL ||
            insnType == AbstractInsnNode.LINE ||
            insnType == AbstractInsnNode.FRAME ||
            insnOpcode == Opcodes.NOP
        ) {
            visitNopInsn(insnNode, currentlyAnalyzing, insnIndex)
        } else {
            current.init(currentlyAnalyzing)
            if (insnOpcode != Opcodes.RETURN) {
                // Don't care about possibly incompatible return type
                current.execute(insnNode, interpreter)
            }
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
                if (handler.maxStackSize > 0) {
                    handler.clearStack()
                    handler.push(interpreter.newExceptionValue(tcb, handler, exnType))
                }
                mergeControlFlowEdge(jump, handler)
            }
        }
    }

    private fun checkAssertions() {
        if (method.instructions.any { it.opcode == Opcodes.JSR || it.opcode == Opcodes.RET })
            throw AssertionError("Subroutines are deprecated since Java 6")
    }

    private fun computeExceptionHandlers(m: MethodNode) {
        for (tcb in m.tryCatchBlocks) {
            if (useFastComputeExceptionHandlers) computeExceptionHandlerFast(tcb) else computeExceptionHandlersForEachInsn(tcb)
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

    protected open fun beforeAnalyze() {}

    private fun initLocals(current: F) {
        current.setReturn(interpreter.newReturnTypeValue(Type.getReturnType(method.desc)))
        val args = Type.getArgumentTypes(method.desc)
        var local = 0
        val isInstanceMethod = (method.access and Opcodes.ACC_STATIC) == 0
        if (isInstanceMethod) {
            val ctype = Type.getObjectType(owner)
            current.setLocal(local, interpreter.newParameterValue(true, local, ctype))
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

    protected open fun visitControlFlowEdge(insnNode: AbstractInsnNode, successor: Int): Boolean = true

    private fun processControlFlowEdge(current: F, insnNode: AbstractInsnNode, jump: Int, canReuse: Boolean = false) {
        if (visitControlFlowEdge(insnNode, jump)) {
            mergeControlFlowEdge(jump, current, canReuse)
        }
    }

    private fun mergeControlFlowEdge(dest: Int, frame: F, canReuse: Boolean = false) {
        if (useFastMergeControlFlowEdge) {
            fastMergeControlFlowEdge(dest, frame, canReuse)
        } else {
            fullMergeControlFlowEdge(dest, frame, canReuse)
        }
    }

    /**
     * Updates frame at the index [dest] with its old value if provided and previous control flow node frame [frame].
     * Reuses old frame when possible and when [canReuse] is true.
     * If updated, adds the frame to the queue
     */
    private fun fullMergeControlFlowEdge(dest: Int, frame: F, canReuse: Boolean = false) {
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
            else -> try {
                oldFrame.merge(frame, interpreter)
            } catch (e: AnalyzerException) {
                throw AnalyzerException(null, "${e.message}\nframe: ${frame.dump()}\noldFrame: ${oldFrame.dump()}")
            }
        }
        updateQueue(changes, dest)
    }

    private fun fastMergeControlFlowEdge(dest: Int, frame: F, canReuse: Boolean) {
        val oldFrame = frames[dest]
        val changes = when {
            oldFrame == null -> {
                frames[dest] = if (canReuse && !isMergeNode[dest]) {
                    frame
                } else {
                    newFrame(frame.locals, frame.maxStackSize).apply { init(frame) }
                }
                true
            }
            else -> false
        }
        updateQueue(changes, dest)
    }

    private fun updateQueue(changes: Boolean, dest: Int) {
        if (changes && !queued[dest]) {
            queued[dest] = true
            queue[top++] = dest
        }
    }

    private fun visitMeaningfulInstruction(insnNode: AbstractInsnNode, insnType: Int, insnOpcode: Int, current: F, insn: Int) {
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

    private fun visitNopInsn(insnNode: AbstractInsnNode, current: F, insn: Int) {
        processControlFlowEdge(current, insnNode, insn + 1, canReuse = true)
    }

    private fun visitOpInsn(insnNode: AbstractInsnNode, current: F, insn: Int) {
        processControlFlowEdge(current, insnNode, insn + 1)
    }

    private fun visitTableSwitchInsnNode(insnNode: TableSwitchInsnNode, current: F) {
        processControlFlowEdge(current, insnNode, insnNode.dflt.indexOf())
        // In most cases order of visiting switch labels should not matter
        // The only one is a tableswitch being added in the beginning of coroutine method, these switch' labels may lead
        // in the middle of try/catch block, and FastAnalyzer is not ready for this (trying to restore stack before it was saved)
        // So we just fix the order of labels being traversed: the first one should be one at the method beginning
        // Using 'asReversed' is because nodes are processed in LIFO order
        for (label in insnNode.labels.asReversed()) {
            processControlFlowEdge(current, insnNode, label.indexOf())
        }
    }

    private fun visitLookupSwitchInsnNode(insnNode: LookupSwitchInsnNode, current: F) {
        processControlFlowEdge(current, insnNode, insnNode.dflt.indexOf())
        for (label in insnNode.labels) {
            processControlFlowEdge(current, insnNode, label.indexOf())
        }
    }

    private fun visitJumpInsnNode(insnNode: JumpInsnNode, current: F, insn: Int, insnOpcode: Int) {
        if (insnOpcode != Opcodes.GOTO) {
            processControlFlowEdge(current, insnNode, insn + 1)
        }
        processControlFlowEdge(current, insnNode, insnNode.label.indexOf())
    }

    protected fun AbstractInsnNode.indexOf() =
        method.instructions.indexOf(this)

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
                when (insn.nodeType) {
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