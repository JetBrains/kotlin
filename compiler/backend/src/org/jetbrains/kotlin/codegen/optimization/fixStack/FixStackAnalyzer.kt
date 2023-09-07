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
 */

package org.jetbrains.kotlin.codegen.optimization.fixStack

import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.codegen.inline.insnText
import org.jetbrains.kotlin.codegen.inline.isAfterInlineMarker
import org.jetbrains.kotlin.codegen.inline.isBeforeInlineMarker
import org.jetbrains.kotlin.codegen.pseudoInsns.PseudoInsn
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.JumpInsnNode
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter
import kotlin.math.max

internal class FixStackAnalyzer(
    owner: String,
    method: MethodNode,
    val context: FixStackContext,
    private val skipBreakContinueGotoEdges: Boolean
) {
    companion object {
        // Stack size is always non-negative
        const val DEAD_CODE_STACK_SIZE = -1
    }

    private val analyzer = object : FastStackAnalyzer<FixStackValue, FixStackAnalyzer.FixStackFrame>(
        owner, method, FixStackInterpreter(), { nLocals, nStack -> FixStackFrame(nLocals, nStack) }
    ) {
        override fun visitControlFlowEdge(insnNode: AbstractInsnNode, successor: Int): Boolean {
            return !(skipBreakContinueGotoEdges && insnNode is JumpInsnNode && context.breakContinueGotoNodes.contains(insnNode))
        }
    }

    private val loopEntryPointMarkers = hashMapOf<LabelNode, SmartList<AbstractInsnNode>>()

    var maxExtraStackSize = 0; private set
    private val spilledStacks = hashMapOf<AbstractInsnNode, List<FixStackValue>>()

    fun analyze() {
        recordLoopEntryPointMarkers()
        analyzer.analyze()
    }

    fun getStackToSpill(location: AbstractInsnNode): List<FixStackValue>? =
        spilledStacks[location]

    fun getActualStack(location: AbstractInsnNode): List<FixStackValue>? =
        analyzer.getFrame(location)?.getStackContent()

    fun getActualStackSize(location: AbstractInsnNode): Int =
        analyzer.getFrame(location)?.stackSizeWithExtra ?: DEAD_CODE_STACK_SIZE

    fun getExpectedStackSize(location: AbstractInsnNode): Int {
        // We should look for expected stack size at loop entry point markers if available,
        // otherwise at location itself.
        val expectedStackSizeNodes = loopEntryPointMarkers[location] ?: listOf(location)

        // Find 1st live node among expected stack size nodes and return corresponding stack size
        for (node in expectedStackSizeNodes) {
            val frame = analyzer.getFrame(node) ?: continue
            return frame.stackSizeWithExtra
        }

        // No live nodes found
        // => loop entry point is unreachable or node itself is unreachable
        return DEAD_CODE_STACK_SIZE
    }

    private fun recordLoopEntryPointMarkers() {
        // NB JVM_IR can generate nested loops with same exit labels (see kt37370.kt)
        for (marker in context.fakeAlwaysFalseIfeqMarkers) {
            val next = marker.next
            if (next is JumpInsnNode) {
                loopEntryPointMarkers.getOrPut(next.label) { SmartList() }.add(marker)
            }
        }
    }

    inner class FixStackFrame(nLocals: Int, nStack: Int) : Frame<FixStackValue>(nLocals, nStack) {
        private val extraStack = Stack<FixStackValue>()

        override fun init(src: Frame<out FixStackValue>): Frame<FixStackValue> {
            extraStack.clear()
            extraStack.addAll((src as FixStackFrame).extraStack)
            return super.init(src)
        }

        override fun clearStack() {
            extraStack.clear()
            super.clearStack()
        }

        override fun execute(insn: AbstractInsnNode, interpreter: Interpreter<FixStackValue>) {
            when {
                PseudoInsn.SAVE_STACK_BEFORE_TRY.isa(insn) ->
                    executeSaveStackBeforeTry(insn)
                PseudoInsn.RESTORE_STACK_IN_TRY_CATCH.isa(insn) ->
                    executeRestoreStackInTryCatch(insn)
                isBeforeInlineMarker(insn) ->
                    executeBeforeInlineCallMarker(insn)
                isAfterInlineMarker(insn) ->
                    executeAfterInlineCallMarker(insn)
                insn.opcode == Opcodes.RETURN ->
                    return
            }

            super.execute(insn, interpreter)
        }

        val stackSizeWithExtra: Int get() = super.getStackSize() + extraStack.size

        fun getStackContent(): List<FixStackValue> {
            val savedStack = ArrayList<FixStackValue>()
            for (i in 0 until super.getStackSize()) {
                savedStack.add(super.getStack(i))
            }
            savedStack.addAll(extraStack)
            return savedStack
        }

        override fun push(value: FixStackValue) {
            if (super.getStackSize() < maxStackSize) {
                super.push(value)
            } else {
                extraStack.add(value)
                maxExtraStackSize = max(maxExtraStackSize, extraStack.size)
            }
        }

        private fun pushAll(values: Collection<FixStackValue>) {
            values.forEach { push(it) }
        }

        override fun pop(): FixStackValue =
            if (extraStack.isNotEmpty()) {
                extraStack.pop()
            } else {
                super.pop()
            }

        override fun setStack(i: Int, value: FixStackValue) {
            if (i < super.getMaxStackSize()) {
                super.setStack(i, value)
            } else {
                extraStack[i - maxStackSize] = value
            }
        }

        override fun merge(frame: Frame<out FixStackValue>, interpreter: Interpreter<FixStackValue>): Boolean {
            throw UnsupportedOperationException("Stack normalization should not merge frames")
        }

        private fun executeBeforeInlineCallMarker(insn: AbstractInsnNode) {
            saveStackAndClear(insn)
        }

        private fun saveStackAndClear(insn: AbstractInsnNode) {
            val savedValues = getStackContent()
            spilledStacks[insn] = savedValues
            clearStack()
        }

        private fun executeAfterInlineCallMarker(insn: AbstractInsnNode) {
            val beforeInlineMarker = context.openingInlineMethodMarker[insn]
            if (stackSize > 0) {
                val returnValue = pop()
                clearStack()
                val savedValues = spilledStacks[beforeInlineMarker]
                pushAll(savedValues!!)
                push(returnValue)
            } else {
                val savedValues = spilledStacks[beforeInlineMarker]
                pushAll(savedValues!!)
            }
        }

        private fun executeRestoreStackInTryCatch(insn: AbstractInsnNode) {
            val saveNode = context.saveStackMarkerForRestoreMarker[insn]
            val savedValues = spilledStacks.getOrElse(saveNode!!) {
                throw AssertionError("${insn.insnText}: Restore stack is unavailable for ${saveNode.insnText}")
            }
            pushAll(savedValues)
        }

        private fun executeSaveStackBeforeTry(insn: AbstractInsnNode) {
            saveStackAndClear(insn)
        }
    }
}
