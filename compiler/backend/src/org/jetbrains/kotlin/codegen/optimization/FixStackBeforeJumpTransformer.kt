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

package org.jetbrains.kotlin.codegen.optimization

import org.jetbrains.kotlin.codegen.optimization.common.MethodAnalyzer
import org.jetbrains.kotlin.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.codegen.pseudoInsns.PseudoInsnOpcode
import org.jetbrains.kotlin.codegen.pseudoInsns.parseOrNull
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.*
import java.util.*
import kotlin.properties.Delegates
import kotlin.test.assertEquals

class FixStackBeforeJumpTransformer : MethodTransformer() {
    public override fun transform(internalClassName: String, methodNode: MethodNode) {
        val jumpsToFix = linkedSetOf<JumpInsnNode>()
        val fakesToGotos = arrayListOf<AbstractInsnNode>()
        val fakesToRemove = arrayListOf<AbstractInsnNode>()

        methodNode.instructions.forEach { insnNode ->
            when {
                PseudoInsnOpcode.FIX_STACK_BEFORE_JUMP.isa(insnNode) -> {
                    val next = insnNode.getNext()
                    assert(next.getOpcode() == Opcodes.GOTO,
                           "Instruction after ${PseudoInsnOpcode.FIX_STACK_BEFORE_JUMP} should be GOTO")
                    jumpsToFix.add(next as JumpInsnNode)
                }
                PseudoInsnOpcode.FAKE_ALWAYS_TRUE_IFEQ.isa(insnNode) -> {
                    assert(insnNode.getNext().getOpcode() == Opcodes.IFEQ,
                           "Instruction after ${PseudoInsnOpcode.FAKE_ALWAYS_TRUE_IFEQ} should be IFEQ")
                    fakesToGotos.add(insnNode)
                }
                PseudoInsnOpcode.FAKE_ALWAYS_FALSE_IFEQ.isa(insnNode) -> {
                    assert(insnNode.getNext().getOpcode() == Opcodes.IFEQ,
                           "Instruction after ${PseudoInsnOpcode.FAKE_ALWAYS_FALSE_IFEQ} should be IFEQ")
                    fakesToRemove.add(insnNode)
                }
            }
        }

        if (jumpsToFix.isEmpty() && fakesToGotos.isEmpty() && fakesToRemove.isEmpty()) {
            return
        }

        if (jumpsToFix.isNotEmpty()) {
            val analyzer = StackDepthAnalyzer(internalClassName, methodNode, jumpsToFix)
            val frames = analyzer.analyze()

            val actions = arrayListOf<() -> Unit>()

            for (jumpNode in jumpsToFix) {
                val jumpIndex = methodNode.instructions.indexOf(jumpNode)
                val labelIndex = methodNode.instructions.indexOf(jumpNode.label)

                val DEAD_CODE = -1 // Stack size is always non-negative for live code
                val actualStackSize = frames[jumpIndex]?.getStackSize() ?: DEAD_CODE
                val expectedStackSize = frames[labelIndex]?.getStackSize() ?: DEAD_CODE

                if (actualStackSize != DEAD_CODE && expectedStackSize != DEAD_CODE) {
                    assert(expectedStackSize <= actualStackSize,
                           "Label at $labelIndex, jump at $jumpIndex: stack underflow: $expectedStackSize > $actualStackSize")
                    val frame = frames[jumpIndex]!!
                    actions.add({ replaceMarkerWithPops(methodNode, jumpNode.getPrevious(), expectedStackSize, frame) })
                }
                else if (actualStackSize != DEAD_CODE && expectedStackSize == DEAD_CODE) {
                    throw AssertionError("Live jump $jumpIndex to dead label $labelIndex")
                }
                else {
                    val marker = jumpNode.getPrevious()
                    actions.add({ methodNode.instructions.remove(marker) })
                }
            }

            actions.forEach { it() }
        }

        for (marker in fakesToGotos) {
            replaceAlwaysTrueIfeqWithGoto(methodNode, marker)
        }

        for (marker in fakesToRemove) {
            removeAlwaysFalseIfeq(methodNode, marker)
        }
    }

    private fun removeAlwaysFalseIfeq(methodNode: MethodNode, nodeToReplace: AbstractInsnNode) {
        with (methodNode.instructions) {
            remove(nodeToReplace.getNext())
            remove(nodeToReplace)
        }
    }

    private fun replaceAlwaysTrueIfeqWithGoto(methodNode: MethodNode, nodeToReplace: AbstractInsnNode) {
        with (methodNode.instructions) {
            val next = nodeToReplace.getNext() as JumpInsnNode
            insertBefore(nodeToReplace, JumpInsnNode(Opcodes.GOTO, next.label))
            remove(nodeToReplace)
            remove(next)
        }
    }

    private fun replaceMarkerWithPops(methodNode: MethodNode, nodeToReplace: AbstractInsnNode, expectedStackSize: Int, frame: Frame<BasicValue>) {
        with (methodNode.instructions) {
            while (frame.getStackSize() > expectedStackSize) {
                val top = frame.pop()
                insertBefore(nodeToReplace, getPopInstruction(top))
            }
            remove(nodeToReplace)
        }
    }

    private fun getPopInstruction(top: BasicValue) =
            InsnNode(when (top.getSize()) {
                         1 -> Opcodes.POP
                         2 -> Opcodes.POP2
                         else -> throw AssertionError("Unexpected value type size")
                     })

    private class StackDepthAnalyzer(
            owner: String,
            methodNode: MethodNode,
            val markedJumps: Set<JumpInsnNode>
    ) : MethodAnalyzer<BasicValue>(owner, methodNode, OptimizationBasicInterpreter()) {
        protected override fun visitControlFlowEdge(insn: Int, successor: Int): Boolean {
            val insnNode = instructions[insn]
            return !(insnNode is JumpInsnNode && markedJumps.contains(insnNode))
        }
    }

}

private inline fun InsnList.forEach(block: (AbstractInsnNode) -> Unit) {
    val iter = this.iterator()
    while (iter.hasNext()) {
        val insn = iter.next()
        block(insn)
    }
}

