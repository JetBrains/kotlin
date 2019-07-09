/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.optimization.boxing

import org.jetbrains.kotlin.codegen.optimization.OptimizationMethodVisitor
import org.jetbrains.kotlin.codegen.optimization.common.debugText
import org.jetbrains.kotlin.codegen.optimization.common.isLoadOperation
import org.jetbrains.kotlin.codegen.optimization.common.isMeaningful
import org.jetbrains.kotlin.codegen.optimization.fixStack.peekWords
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.removeNodeGetNext
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.Analyzer
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceInterpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceValue
import java.util.*

class PopBackwardPropagationTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        if (!OptimizationMethodVisitor.canBeOptimizedUsingSourceInterpreter(methodNode)) return
        Transformer(methodNode).transform()
    }

    private class Transformer(val methodNode: MethodNode) {
        private interface Transformation {
            fun apply(insn: AbstractInsnNode)
        }

        private inline fun Transformation(crossinline body: (AbstractInsnNode) -> Unit): Transformation =
            object : Transformation {
                override fun apply(insn: AbstractInsnNode) {
                    body(insn)
                }
            }

        private val REPLACE_WITH_NOP = Transformation { insnList.set(it, createRemovableNopInsn()) }
        private val REPLACE_WITH_POP1 = Transformation { insnList.set(it, InsnNode(Opcodes.POP)) }
        private val REPLACE_WITH_POP2 = Transformation { insnList.set(it, InsnNode(Opcodes.POP2)) }
        private val INSERT_POP1_AFTER = Transformation { insnList.insert(it, InsnNode(Opcodes.POP)) }
        private val INSERT_POP2_AFTER = Transformation { insnList.insert(it, InsnNode(Opcodes.POP2)) }

        private val insnList = methodNode.instructions

        private val insns = insnList.toArray()

        private val dontTouchInsnIndices = BitSet(insns.size)
        private val transformations = hashMapOf<AbstractInsnNode, Transformation>()
        private val removableNops = hashSetOf<InsnNode>()

        private val frames by lazy { analyzeMethodBody() }

        fun transform() {
            if (insns.none { it.isPop() || it.isPurePush() }) return

            computeTransformations()
            for ((insn, transformation) in transformations.entries) {
                transformation.apply(insn)
            }
            postprocessNops()
        }

        private fun analyzeMethodBody(): Array<out Frame<SourceValue>?> {
            val frames = Analyzer<SourceValue>(HazardsTrackingInterpreter()).analyze("fake", methodNode)

            postprocessStackHazards(frames)

            return frames
        }

        private fun postprocessStackHazards(frames: Array<out Frame<SourceValue>?>) {
            val insns = methodNode.instructions.toArray()
            for (i in frames.indices) {
                val frame = frames[i] ?: continue
                val insn = insns[i]

                when (insn.opcode) {
                    Opcodes.POP2 -> {
                        val top2 = frame.peekWords(2) ?: throwIncorrectBytecode(insn, frame)
                        top2.forEach { it.insns.markAsDontTouch() }
                    }
                    Opcodes.DUP_X1 -> {
                        val top2 = frame.peekWords(1, 1) ?: throwIncorrectBytecode(insn, frame)
                        top2.forEach { it.insns.markAsDontTouch() }
                    }
                    Opcodes.DUP2_X1 -> {
                        val top3 = frame.peekWords(2, 1) ?: throwIncorrectBytecode(insn, frame)
                        top3.forEach { it.insns.markAsDontTouch() }
                    }
                    Opcodes.DUP_X2 -> {
                        val top3 = frame.peekWords(1, 2) ?: throwIncorrectBytecode(insn, frame)
                        top3.forEach { it.insns.markAsDontTouch() }
                    }
                    Opcodes.DUP2_X2 -> {
                        val top4 = frame.peekWords(2, 2) ?: throwIncorrectBytecode(insn, frame)
                        top4.forEach { it.insns.markAsDontTouch() }
                    }
                }
            }
        }

        private fun throwIncorrectBytecode(insn: AbstractInsnNode?, frame: Frame<SourceValue>): Nothing {
            throw AssertionError("Incorrect bytecode at ${methodNode.instructions.indexOf(insn)}: ${insn.debugText} $frame")
        }

        private inner class HazardsTrackingInterpreter : SourceInterpreter(Opcodes.API_VERSION) {
            override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out SourceValue>): SourceValue {
                for (value in values) {
                    value.insns.markAsDontTouch()
                }
                return super.naryOperation(insn, values)
            }

            override fun copyOperation(insn: AbstractInsnNode, value: SourceValue): SourceValue {
                value.insns.markAsDontTouch()
                return super.copyOperation(insn, value)
            }

            override fun unaryOperation(insn: AbstractInsnNode, value: SourceValue): SourceValue {
                if (insn.opcode != Opcodes.CHECKCAST && !insn.isPrimitiveTypeConversion()) {
                    value.insns.markAsDontTouch()
                }
                return super.unaryOperation(insn, value)
            }

            override fun binaryOperation(insn: AbstractInsnNode, value1: SourceValue, value2: SourceValue): SourceValue {
                value1.insns.markAsDontTouch()
                value2.insns.markAsDontTouch()
                return super.binaryOperation(insn, value1, value2)
            }

            override fun ternaryOperation(
                insn: AbstractInsnNode,
                value1: SourceValue,
                value2: SourceValue,
                value3: SourceValue
            ): SourceValue {
                value1.insns.markAsDontTouch()
                value2.insns.markAsDontTouch()
                value3.insns.markAsDontTouch()
                return super.ternaryOperation(insn, value1, value2, value3)
            }
        }

        private fun Collection<AbstractInsnNode>.markAsDontTouch() {
            forEach {
                dontTouchInsnIndices[insnList.indexOf(it)] = true
            }
        }


        private fun computeTransformations() {
            transformations.clear()

            for (i in insns.indices) {
                if (frames[i] == null) continue
                val insn = insns[i]

                if (insn.opcode == Opcodes.POP) {
                    propagatePopBackwards(insn, 0)
                }
            }
        }

        private fun propagatePopBackwards(insn: AbstractInsnNode, poppedValueSize: Int) {
            if (transformations.containsKey(insn)) return

            when {
                insn.opcode == Opcodes.POP -> {
                    val inputTop = getInputTop(insn)
                    val sources = inputTop.insns
                    if (sources.all { !isDontTouch(it) } && sources.any { isTransformablePopOperand(it) }) {
                        transformations[insn] = replaceWithNopTransformation()
                        sources.forEach { propagatePopBackwards(it, inputTop.size) }
                    }
                }

                insn.opcode == Opcodes.CHECKCAST -> {
                    val inputTop = getInputTop(insn)
                    val sources = inputTop.insns
                    val resultType = (insn as TypeInsnNode).desc
                    if (sources.all { !isDontTouch(it) } && sources.any { isTransformableCheckcastOperand(it, resultType) }) {
                        transformations[insn] = replaceWithNopTransformation()
                        sources.forEach { propagatePopBackwards(it, inputTop.size) }
                    } else {
                        transformations[insn] = insertPopAfterTransformation(poppedValueSize)
                    }
                }

                insn.isPrimitiveBoxing() -> {
                    val boxedValueSize = getInputTop(insn).size
                    transformations[insn] = replaceWithPopTransformation(boxedValueSize)
                }

                insn.isPurePush() -> {
                    transformations[insn] = replaceWithNopTransformation()
                }

                insn.isPrimitiveTypeConversion() -> {
                    val inputTop = getInputTop(insn)
                    val sources = inputTop.insns
                    if (sources.all { !isDontTouch(it) }) {
                        transformations[insn] = replaceWithNopTransformation()
                        sources.forEach { propagatePopBackwards(it, inputTop.size) }
                    } else {
                        transformations[insn] = replaceWithPopTransformation(poppedValueSize)
                    }
                }

                else -> {
                    transformations[insn] = insertPopAfterTransformation(poppedValueSize)
                }
            }
        }

        private fun postprocessNops() {
            var node: AbstractInsnNode? = insnList.first
            var hasRemovableNops = false
            while (node != null) {
                node = node.next
                val begin = node ?: break
                while (node != null && node !is LabelNode) {
                    if (node in removableNops) {
                        hasRemovableNops = true
                    }
                    node = node.next
                }
                val end = node
                if (hasRemovableNops) {
                    removeUnneededNopsInRange(begin, end)
                }
                hasRemovableNops = false
            }
        }

        private fun removeUnneededNopsInRange(begin: AbstractInsnNode, end: AbstractInsnNode?) {
            var node: AbstractInsnNode? = begin
            var keepNop = true
            while (node != null && node != end) {
                if (node in removableNops && !keepNop) {
                    node = insnList.removeNodeGetNext(node)
                } else {
                    if (node.isMeaningful) keepNop = false
                    node = node.next
                }
            }
        }

        private fun replaceWithPopTransformation(size: Int): Transformation =
            when (size) {
                1 -> REPLACE_WITH_POP1
                2 -> REPLACE_WITH_POP2
                else -> throw AssertionError("Unexpected pop value size: $size")
            }

        private fun insertPopAfterTransformation(size: Int): Transformation =
            when (size) {
                1 -> INSERT_POP1_AFTER
                2 -> INSERT_POP2_AFTER
                else -> throw AssertionError("Unexpected pop value size: $size")
            }

        private fun replaceWithNopTransformation(): Transformation =
            REPLACE_WITH_NOP

        private fun createRemovableNopInsn() =
            InsnNode(Opcodes.NOP).apply { removableNops.add(this) }

        private fun getInputTop(insn: AbstractInsnNode): SourceValue {
            val i = insnList.indexOf(insn)
            val frame = frames[i] ?: throw AssertionError("Unexpected dead instruction #$i")
            return frame.top() ?: throw AssertionError("Instruction #$i has empty stack on input")
        }

        private fun isTransformableCheckcastOperand(it: AbstractInsnNode, resultType: String) =
            it.isPrimitiveBoxing() && (it as MethodInsnNode).owner == resultType

        private fun isTransformablePopOperand(insn: AbstractInsnNode) =
            insn.opcode == Opcodes.CHECKCAST || insn.isPrimitiveBoxing() || insn.isPurePush()

        private fun isDontTouch(insn: AbstractInsnNode) =
            dontTouchInsnIndices[insnList.indexOf(insn)]
    }

}

fun AbstractInsnNode.isPurePush() =
    isLoadOperation() ||
            opcode in Opcodes.ACONST_NULL..Opcodes.LDC + 2 ||
            isUnitInstance()

fun AbstractInsnNode.isPop() =
    opcode == Opcodes.POP || opcode == Opcodes.POP2

fun AbstractInsnNode.isUnitInstance() =
    opcode == Opcodes.GETSTATIC &&
            this is FieldInsnNode && owner == "kotlin/Unit" && name == "INSTANCE"

fun AbstractInsnNode.isPrimitiveTypeConversion() =
    opcode in Opcodes.I2L..Opcodes.I2S
