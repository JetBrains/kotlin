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
import org.jetbrains.kotlin.codegen.inline.isMarkedReturn
import org.jetbrains.kotlin.codegen.optimization.common.MethodAnalyzer
import org.jetbrains.kotlin.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.kotlin.codegen.pseudoInsns.PseudoInsn
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter

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

    private val expectedStackNode = hashMapOf<LabelNode, AbstractInsnNode>()

    val maxExtraStackSize: Int get() = analyzer.maxExtraStackSize

    fun getStackToSpill(location: AbstractInsnNode) = analyzer.spilledStacks[location]
    fun getActualStack(location: AbstractInsnNode) = getFrame(location)?.getStackContent()
    fun getActualStackSize(location: AbstractInsnNode) = getFrame(location)?.stackSizeWithExtra ?: DEAD_CODE_STACK_SIZE
    fun getExpectedStackSize(location: AbstractInsnNode) = getExpectedStackFrame(location)?.stackSizeWithExtra ?: DEAD_CODE_STACK_SIZE

    private fun getExpectedStackFrame(location: AbstractInsnNode) = getFrame(expectedStackNode[location] ?: location)
    private fun getFrame(location: AbstractInsnNode) = analyzer.getFrame(location) as? InternalAnalyzer.FixStackFrame

    fun analyze() {
        preprocess()
        analyzer.analyze()
    }

    private fun preprocess() {
        for (marker in context.fakeAlwaysFalseIfeqMarkers) {
            val next = marker.next
            if (next is JumpInsnNode) {
                expectedStackNode[next.label] = marker
            }
        }
    }

    private val analyzer = InternalAnalyzer(owner)

    private inner class InternalAnalyzer(owner: String) : MethodAnalyzer<BasicValue>(owner, method, OptimizationBasicInterpreter()) {
        val spilledStacks = hashMapOf<AbstractInsnNode, List<BasicValue>>()
        var maxExtraStackSize = 0; private set

        override fun visitControlFlowEdge(insn: Int, successor: Int): Boolean {
            if (!skipBreakContinueGotoEdges) return true
            val insnNode = instructions[insn]
            return !(insnNode is JumpInsnNode && context.breakContinueGotoNodes.contains(insnNode))
        }

        override fun newFrame(nLocals: Int, nStack: Int): Frame<BasicValue> =
                FixStackFrame(nLocals, nStack)

        private fun indexOf(node: AbstractInsnNode) = method.instructions.indexOf(node)

        inner class FixStackFrame(nLocals: Int, nStack: Int) : Frame<BasicValue>(nLocals, nStack) {
            val extraStack = Stack<BasicValue>()

            override fun init(src: Frame<out BasicValue>): Frame<BasicValue> {
                extraStack.clear()
                extraStack.addAll((src as FixStackFrame).extraStack)
                return super.init(src)
            }

            override fun clearStack() {
                extraStack.clear()
                super.clearStack()
            }

            override fun execute(insn: AbstractInsnNode, interpreter: Interpreter<BasicValue>) {
                when {
                    PseudoInsn.SAVE_STACK_BEFORE_TRY.isa(insn) ->
                        executeSaveStackBeforeTry(insn)
                    PseudoInsn.RESTORE_STACK_IN_TRY_CATCH.isa(insn) ->
                        executeRestoreStackInTryCatch(insn)
                    isBeforeInlineMarker(insn) ->
                        executeBeforeInlineCallMarker(insn)
                    isAfterInlineMarker(insn) ->
                        executeAfterInlineCallMarker(insn)
                    isMarkedReturn(insn) -> {
                        // KT-9644: might throw "Incompatible return type" on non-local return, in fact we don't care.
                        if (insn.opcode == Opcodes.RETURN) return
                    }
                }

                super.execute(insn, interpreter)
            }

            val stackSizeWithExtra: Int get() = super.getStackSize() + extraStack.size

            fun getStackContent(): List<BasicValue> {
                val savedStack = arrayListOf<BasicValue>()
                IntRange(0, super.getStackSize() - 1).mapTo(savedStack) { super.getStack(it) }
                savedStack.addAll(extraStack)
                return savedStack
            }

            override fun push(value: BasicValue) {
                if (super.getStackSize() < maxStackSize) {
                    super.push(value)
                }
                else {
                    extraStack.add(value)
                    maxExtraStackSize = Math.max(maxExtraStackSize, extraStack.size)
                }
            }

            fun pushAll(values: Collection<BasicValue>) {
                values.forEach { push(it) }
            }

            override fun pop(): BasicValue {
                return if (extraStack.isNotEmpty()) {
                    extraStack.pop()
                }
                else {
                    super.pop()
                }
            }

            override fun getStack(i: Int): BasicValue {
                return if (i < super.getMaxStackSize()) {
                    super.getStack(i)
                }
                else {
                    extraStack[i - maxStackSize]
                }
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
            }
            else {
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
