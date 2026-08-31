/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.codegen.inline.getLabelToIndexMap
import org.jetbrains.kotlin.codegen.inline.getLineNumberOrNull
import org.jetbrains.kotlin.codegen.inline.insnText
import org.jetbrains.kotlin.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.kotlin.codegen.optimization.common.StrictBasicValue
import org.jetbrains.kotlin.codegen.optimization.common.findNextOrNull
import org.jetbrains.kotlin.codegen.optimization.common.intConstant
import org.jetbrains.kotlin.codegen.optimization.common.nodeType
import org.jetbrains.kotlin.codegen.optimization.fixStack.peek
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame

class ConstantConditionEliminationMethodTransformer : MethodTransformer() {

    override fun transform(internalClassName: String, methodNode: MethodNode) {
        if (!methodNode.hasOptimizableConditions()) {
            return
        }
        do {
            val changes = ConstantConditionsOptimization(internalClassName, methodNode).runOnce()
        } while (changes)
    }

    private fun MethodNode.hasOptimizableConditions(): Boolean {
        return instructions.any { it.isIntJump() } && instructions.any { it.intConstant != null }
    }

    private fun AbstractInsnNode.isIntJump() =
        opcode in Opcodes.IFEQ..Opcodes.IFLE || opcode in Opcodes.IF_ICMPEQ..Opcodes.IF_ICMPLE

    private class ConstantConditionsOptimization(val internalClassName: String, val methodNode: MethodNode) {
        val inlineScopes by lazy(LazyThreadSafetyMode.NONE) { methodNode.computeInlineScopes() }

        fun runOnce(): Boolean {
            var changed = false

            val frames = analyze(internalClassName, methodNode, ConstantPropagationInterpreter())
            val insns = methodNode.instructions.toArray()

            for (i in frames.indices) {
                val insn = insns[i]
                val frame = frames[i]

                if (frame == null) {
                    if (insn !is LabelNode) {
                        methodNode.instructions.remove(insn)
                    }
                    continue
                }

                if (insn !is JumpInsnNode) continue
                when (insn.opcode) {
                    in Opcodes.IFEQ..Opcodes.IFLE ->
                        changed = tryRewriteComparisonWithZero(insn, frame) || changed
                    in Opcodes.IF_ICMPEQ..Opcodes.IF_ICMPLE ->
                        changed = tryRewriteBinaryComparison(insn, frame) || changed
                    Opcodes.GOTO -> {
                        frame.top()?.intConstant?.let {
                            changed = tryRewriteGotoToComparison(insn, it) || changed
                        }
                    }
                }
            }

            return changed
        }

        private fun tryRewriteComparisonWithZero(insn: JumpInsnNode, frame: Frame<BasicValue>): Boolean {
            val top = frame.top()!!.intConstant ?: return false

            if (evaluateComparisonWithZeroInsn(insn.opcode, top)) {
                methodNode.instructions.insertBefore(insn, InsnNode(Opcodes.POP))
                insn.opcode = Opcodes.GOTO
            } else {
                methodNode.instructions.set(insn, InsnNode(Opcodes.POP))
            }

            return true
        }

        private fun tryRewriteBinaryComparison(insn: JumpInsnNode, frame: Frame<BasicValue>): Boolean {
            val arg2 = frame.peek(0)!!.intConstant ?: return false
            val arg1 = frame.peek(1)!!.intConstant

            if (arg1 != null) {
                rewriteBinaryComparisonOfConsts(insn, arg1, arg2)
                return true
            } else if (arg2 == 0) {
                rewriteBinaryComparisonWith0(insn)
                return true
            }

            return false
        }

        private fun rewriteBinaryComparisonOfConsts(insn: JumpInsnNode, value1: Int, value2: Int) {
            val constCondition = when (insn.opcode) {
                Opcodes.IF_ICMPEQ -> value1 == value2
                Opcodes.IF_ICMPNE -> value1 != value2
                Opcodes.IF_ICMPLE -> value1 <= value2
                Opcodes.IF_ICMPLT -> value1 < value2
                Opcodes.IF_ICMPGE -> value1 >= value2
                Opcodes.IF_ICMPGT -> value1 > value2
                else -> throw AssertionError("Unexpected instruction: ${insn.insnText}")
            }

            if (constCondition) {
                methodNode.instructions.insertBefore(insn, InsnNode(Opcodes.POP))
                methodNode.instructions.insertBefore(insn, InsnNode(Opcodes.POP))
                insn.opcode = Opcodes.GOTO
            } else {
                methodNode.instructions.insertBefore(insn, InsnNode(Opcodes.POP))
                methodNode.instructions.set(insn, InsnNode(Opcodes.POP))
            }
        }

        private fun rewriteBinaryComparisonWith0(insn: JumpInsnNode) {
            require(insn.opcode in Opcodes.IF_ICMPEQ..Opcodes.IF_ICMPLE)
            methodNode.instructions.insertBefore(insn, InsnNode(Opcodes.POP))
            // Map the range of opcodes IF_CMPEQ..IF_ICMPLE -> IFEQ..IFLE (IF_CMPEQ -> IFEQ, IFNE -> IF_CMPNE, etc.)
            insn.opcode = Opcodes.IFEQ + (insn.opcode - Opcodes.IF_ICMPEQ)
        }

        private fun tryRewriteGotoToComparison(insn: JumpInsnNode, top: Int): Boolean {
            val targetInsn = insn.targetExecutableInsn() ?: return false
            if (targetInsn.opcode !in Opcodes.IFEQ..Opcodes.IFLE) return false

            // Do not optimize jumps across inline scopes, as it messes debug information.
            if (inlineScopes[insn] !== inlineScopes[targetInsn]) return false

            methodNode.instructions.insertBefore(insn, InsnNode(Opcodes.POP))

            getLineNumberOrNull(targetInsn)?.takeUnless { it == getLineNumberOrNull(insn) }?.let { lineNumber ->
                val lineNumberLabel = LabelNode()
                methodNode.instructions.insertBefore(insn, lineNumberLabel)
                methodNode.instructions.insertBefore(insn, LineNumberNode(lineNumber, lineNumberLabel))
            }

            insn.label = when (evaluateComparisonWithZeroInsn(targetInsn.opcode, top)) {
                true -> (targetInsn as JumpInsnNode).label
                false -> methodNode.instructions.getOrCreateLabelAfter(targetInsn)
            }

            return true
        }
    }
}

private class IConstValue private constructor(val value: Int) : StrictBasicValue(Type.INT_TYPE) {
    override fun equals(other: Any?): Boolean = other is IConstValue && other.value == this.value
    override fun hashCode(): Int = value
    override fun toString(): String = "IConst($value)"

    companion object {
        private val ICONST_CACHE = Array(7) { IConstValue(it - 1) }

        fun of(value: Int) =
            if (value in -1..5)
                ICONST_CACHE[value + 1]
            else
                IConstValue(value)
    }
}

private class ConstantPropagationInterpreter : OptimizationBasicInterpreter() {
    override fun newOperation(insn: AbstractInsnNode): BasicValue {
        insn.intConstant?.let { return IConstValue.of(it) }
        return super.newOperation(insn)
    }

    override fun merge(v: BasicValue, w: BasicValue): BasicValue =
        if (v is IConstValue && w is IConstValue && v == w)
            v
        else
            super.merge(v, w)
}

private fun evaluateComparisonWithZeroInsn(opcode: Int, value: Int): Boolean =
    when (opcode) {
        Opcodes.IFEQ -> value == 0
        Opcodes.IFNE -> value != 0
        Opcodes.IFGE -> value >= 0
        Opcodes.IFGT -> value > 0
        Opcodes.IFLE -> value <= 0
        Opcodes.IFLT -> value < 0
        else -> throw AssertionError("Unexpected instruction opcode: $opcode")
    }

private fun InsnList.getOrCreateLabelAfter(insn: AbstractInsnNode): LabelNode =
    insn.findNextOrNull { it.opcode != Opcodes.NOP && it.nodeType != AbstractInsnNode.LINE } as? LabelNode
        ?: LabelNode().also { insert(insn, it) }

private fun JumpInsnNode.targetExecutableInsn(): AbstractInsnNode? {
    var target: AbstractInsnNode? = label
    val visitedGoto = HashSet<JumpInsnNode>()
    while (target != null) {
        target = when {
            target.opcode == Opcodes.GOTO -> {
                require(target is JumpInsnNode)
                if (!visitedGoto.add(target)) {
                    // Goto already visited -> infinite loop
                    return null
                }
                target.label
            }
            target.opcode == Opcodes.NOP || target.nodeType == AbstractInsnNode.LABEL || target.nodeType == AbstractInsnNode.LINE -> target.next
            else -> break
        }
    }
    return target
}

private val BasicValue.intConstant: Int?
    get() = when (this) {
        is IConstValue -> value
        else -> null
    }

/**
 * Maps every instruction inside an inline scope to the innermost inline marker variable.
 */
private fun MethodNode.computeInlineScopes(): Map<AbstractInsnNode, LocalVariableNode> {
    val markers = localVariables?.filter { JvmAbi.isFakeLocalVariableForInline(it.name) }
    if (markers.isNullOrEmpty()) return emptyMap()

    val labelToIndex = getLabelToIndexMap()
    val opening = HashMap<LabelNode, MutableList<LocalVariableNode>>()
    val closing = HashMap<LabelNode, MutableList<LocalVariableNode>>()

    // Widest first, so that the narrowest ends up on top of `active`
    markers.sortedByDescending { labelToIndex[it.end.label]!! - labelToIndex[it.start.label]!! }.forEach { marker ->
        opening.getOrPut(marker.start) { mutableListOf() }.add(marker)
        closing.getOrPut(marker.end) { mutableListOf() }.add(marker)
    }

    val result = HashMap<AbstractInsnNode, LocalVariableNode>()
    val active = mutableListOf<LocalVariableNode>()
    for (insn in instructions) {
        if (insn is LabelNode) {
            closing[insn]?.let(active::removeAll)
            opening[insn]?.let(active::addAll)
        }
        active.lastOrNull()?.let { result[insn] = it }
    }
    return result
}
