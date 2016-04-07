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

import org.jetbrains.kotlin.codegen.optimization.common.isMeaningful
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.removeNodeGetNext
import org.jetbrains.kotlin.codegen.optimization.replaceNodeGetNext
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.Analyzer
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceInterpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceValue
import org.jetbrains.org.objectweb.asm.util.Printer

class RedundantCoercionToUnitTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        Transformer(methodNode).transform()
    }

    private class Transformer(val methodNode: MethodNode) {
        private val insnList = methodNode.instructions

        private val insns = insnList.toArray()

        private val dontTouchInsns = hashSetOf<AbstractInsnNode>()
        private val transformations = hashMapOf<AbstractInsnNode, () -> Unit>()
        private val removableNops = hashSetOf<InsnNode>()

        private val frames: Array<out Frame<SourceValue>?> = analyzeMethodBody()

        fun transform() {
            computeTransformations()
            transformations.values.forEach { it() }
            postprocessNops()
        }

        private fun analyzeMethodBody(): Array<out Frame<SourceValue>?> =
                Analyzer<SourceValue>(object : SourceInterpreter() {
                    override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out SourceValue>): SourceValue {
                        for (value in values) {
                            dontTouchInsns.addAll(value.insns)
                        }
                        return super.naryOperation(insn, values)
                    }

                    override fun copyOperation(insn: AbstractInsnNode, value: SourceValue): SourceValue {
                        dontTouchInsns.addAll(value.insns)
                        return super.copyOperation(insn, value)
                    }

                    override fun unaryOperation(insn: AbstractInsnNode, value: SourceValue): SourceValue {
                        if (insn.opcode != Opcodes.CHECKCAST) {
                            dontTouchInsns.addAll(value.insns)
                        }
                        return super.unaryOperation(insn, value)
                    }

                    override fun binaryOperation(insn: AbstractInsnNode, value1: SourceValue, value2: SourceValue): SourceValue {
                        dontTouchInsns.addAll(value1.insns)
                        dontTouchInsns.addAll(value2.insns)
                        return super.binaryOperation(insn, value1, value2)
                    }

                    override fun ternaryOperation(insn: AbstractInsnNode, value1: SourceValue, value2: SourceValue, value3: SourceValue): SourceValue {
                        dontTouchInsns.addAll(value1.insns)
                        dontTouchInsns.addAll(value2.insns)
                        dontTouchInsns.addAll(value3.insns)
                        return super.ternaryOperation(insn, value1, value2, value3)
                    }
                }).analyze("fake", methodNode)


        private fun computeTransformations() {
            transformations.clear()

            for (i in 0..insns.lastIndex) {
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
                        transformations[insn] = replaceWithNopTransformation(insn)
                        sources.forEach { propagatePopBackwards(it, inputTop.size) }
                    }
                }

                insn.opcode == Opcodes.CHECKCAST -> {
                    val inputTop = getInputTop(insn)
                    val sources = inputTop.insns
                    val resultType = (insn as TypeInsnNode).desc
                    if (sources.all { !isDontTouch(it) } && sources.any { isTransformableCheckcastOperand(it, resultType) }) {
                        transformations[insn] = replaceWithNopTransformation(insn)
                        sources.forEach { propagatePopBackwards(it, inputTop.size) }
                    }
                    else {
                        transformations[insn] = insertPopAfterTransformation(insn, poppedValueSize)
                    }
                }

                insn.isPrimitiveBoxing() -> {
                    val boxedValueSize = getInputTop(insn).size
                    transformations[insn] = replaceWithPopTransformation(insn, boxedValueSize)
                }

                insn.isUnitOrNull() -> {
                    transformations[insn] = replaceWithNopTransformation(insn)
                }

                else -> {
                    transformations[insn] = insertPopAfterTransformation(insn, poppedValueSize)
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
                }
                else {
                    if (node.isMeaningful) keepNop = false
                    node = node.next
                }
            }
        }

        private fun replaceWithPopTransformation(insn: AbstractInsnNode, size: Int): () -> Unit =
                { insnList.replaceNodeGetNext(insn, createPopInsn(size)) }

        private fun insertPopAfterTransformation(insn: AbstractInsnNode, size: Int) =
                { insnList.insert(insn, createPopInsn(size)) }

        private fun replaceWithNopTransformation(insn: AbstractInsnNode): () -> Unit =
                { insnList.replaceNodeGetNext(insn, createRemovableNopInsn()) }

        private fun createPopInsn(size: Int) =
                when (size) {
                    1 -> InsnNode(Opcodes.POP)
                    2 -> InsnNode(Opcodes.POP2)
                    else -> throw AssertionError("Unexpected popped value size: $size")
                }

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
                insn.opcode == Opcodes.CHECKCAST || insn.isPrimitiveBoxing() || insn.isUnitOrNull()

        private fun isDontTouch(insn: AbstractInsnNode) =
                insn in dontTouchInsns

        private fun throwIllegalStackInsn(i: Int): Nothing =
                throw AssertionError("#$i: illegal use of ${Printer.OPCODES[insns[i].opcode]}, input stack: ${formatInputStack(frames[i])}")

        private fun formatInputStack(frame: Frame<SourceValue>?): String =
                if (frame == null)
                    "unknown (dead code)"
                else
                    (0..frame.stackSize - 1).map { frame.getStack(it).size }.joinToString(prefix =  "[", postfix = "]")
    }

}

fun AbstractInsnNode.isUnitOrNull() =
        opcode == Opcodes.ACONST_NULL ||
        opcode == Opcodes.GETSTATIC && this is FieldInsnNode && owner == "kotlin/Unit" && name == "INSTANCE"
