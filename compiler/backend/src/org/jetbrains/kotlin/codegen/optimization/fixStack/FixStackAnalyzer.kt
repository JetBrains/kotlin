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
import org.jetbrains.kotlin.codegen.inline.isAfterInlineMarker
import org.jetbrains.kotlin.codegen.inline.isBeforeInlineMarker
import org.jetbrains.kotlin.codegen.pseudoInsns.PseudoInsn
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.JumpInsnNode
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter
import kotlin.math.max

internal class FixStackAnalyzer(
    owner: String,
    val method: MethodNode,
    val context: FixStackContext,
    private val skipBreakContinueGotoEdges: Boolean = true
) {
    companion object {
        // Stack size is always non-negative
        const val DEAD_CODE_STACK_SIZE = -1
    }

    private val loopEntryPointMarkers = hashMapOf<LabelNode, SmartList<AbstractInsnNode>>()

    val maxExtraStackSize: Int get() = analyzer.maxExtraStackSize

    fun getStackToSpill(location: AbstractInsnNode): List<FixStackValue>? =
        analyzer.spilledStacks[location]

    fun getActualStack(location: AbstractInsnNode): List<FixStackValue>? =
        getFrame(location)?.getStackContent()

    fun getActualStackSize(location: AbstractInsnNode): Int =
        getFrame(location)?.stackSizeWithExtra ?: DEAD_CODE_STACK_SIZE

    fun getExpectedStackSize(location: AbstractInsnNode): Int {
        // We should look for expected stack size at loop entry point markers if available,
        // otherwise at location itself.
        val expectedStackSizeNodes = loopEntryPointMarkers[location] ?: listOf(location)

        // Find 1st live node among expected stack size nodes and return corresponding stack size
        for (node in expectedStackSizeNodes) {
            val frame = getFrame(node) ?: continue
            return frame.stackSizeWithExtra
        }

        // No live nodes found
        // => loop entry point is unreachable or node itself is unreachable
        return DEAD_CODE_STACK_SIZE
    }

    private fun getFrame(location: AbstractInsnNode) = analyzer.getFrame(location) as? InternalAnalyzer.FixStackFrame

    fun analyze() {
        recordLoopEntryPointMarkers()
        analyzer.analyze()
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

    private val analyzer = InternalAnalyzer(owner)

    private inner class InternalAnalyzer(owner: String) :
        HackedFixStackMethodAnalyzerBase<FixStackValue>(owner, method, FixStackInterpreter()) {

        val spilledStacks = hashMapOf<AbstractInsnNode, List<FixStackValue>>()
        var maxExtraStackSize = 0; private set

        override fun visitControlFlowEdge(insn: Int, successor: Int): Boolean {
            if (!skipBreakContinueGotoEdges) return true
            val insnNode = insnsArray[insn]
            return !(insnNode is JumpInsnNode && context.breakContinueGotoNodes.contains(insnNode))
        }

        override fun newFrame(nLocals: Int, nStack: Int): Frame<FixStackValue> =
            FixStackFrame(nLocals, nStack)

        private fun indexOf(node: AbstractInsnNode) = method.instructions.indexOf(node)

        inner class FixStackFrame(nLocals: Int, nStack: Int) : Frame<FixStackValue>(nLocals, nStack) {
            val extraStack = Stack<FixStackValue>()

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
                IntRange(0, super.getStackSize() - 1).mapTo(savedStack) { super.getStack(it) }
                savedStack.addAll(extraStack)
                return savedStack
            }

            override fun push(value: FixStackValue) {
                if (value == FixStackValue.UNINITIALIZED) {
                    throw AnalyzerException(null, "Uninitialized value on stack")
                }
                if (super.getStackSize() < maxStackSize) {
                    super.push(value)
                } else {
                    extraStack.add(value)
                    maxExtraStackSize = max(maxExtraStackSize, extraStack.size)
                }
            }

            fun pushAll(values: Collection<FixStackValue>) {
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
                val other = frame as FixStackFrame
                if (stackSizeWithExtra != other.stackSizeWithExtra) {
                    throw AnalyzerException(null, "Incompatible stack heights")
                }
                var changed = false
                for (i in 0 until stackSize) {
                    val v0 = super.getStack(i)
                    val vm = interpreter.merge(v0, other.getStack(i))
                    if (vm != v0) {
                        super.setStack(i, vm)
                        changed = true
                    }
                }
                for (i in 0 until extraStack.size) {
                    val v0 = extraStack[i]
                    val vm = interpreter.merge(v0, other.extraStack[i])
                    if (vm != v0) {
                        extraStack[i] = vm
                        changed = true
                    }
                }
                return changed
            }
        }

        private fun FixStackFrame.executeBeforeInlineCallMarker(insn: AbstractInsnNode) {
            saveStackAndClear(insn)
        }

        private fun FixStackFrame.saveStackAndClear(insn: AbstractInsnNode) {
            val savedValues = getStackContent()
            spilledStacks[insn] = savedValues
            clearStack()
        }

        private fun FixStackFrame.executeAfterInlineCallMarker(insn: AbstractInsnNode) {
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

        private fun FixStackFrame.executeRestoreStackInTryCatch(insn: AbstractInsnNode) {
            val saveNode = context.saveStackMarkerForRestoreMarker[insn]
            val savedValues = spilledStacks.getOrElse(saveNode!!) {
                throw AssertionError("${indexOf(insn)}: Restore stack is unavailable for ${indexOf(saveNode)}")
            }
            pushAll(savedValues)
        }

        private fun FixStackFrame.executeSaveStackBeforeTry(insn: AbstractInsnNode) {
            saveStackAndClear(insn)
        }
    }


}
