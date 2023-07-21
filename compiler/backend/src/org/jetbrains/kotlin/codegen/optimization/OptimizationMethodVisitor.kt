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

import org.jetbrains.kotlin.codegen.TransformationMethodVisitor
import org.jetbrains.kotlin.codegen.coroutines.UninitializedStoresProcessor
import org.jetbrains.kotlin.codegen.inline.InplaceArgumentsMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.boxing.PopBackwardPropagationTransformer
import org.jetbrains.kotlin.codegen.optimization.boxing.RedundantBoxingMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.boxing.StackPeepholeOptimizationsTransformer
import org.jetbrains.kotlin.codegen.optimization.common.prepareForEmitting
import org.jetbrains.kotlin.codegen.optimization.nullCheck.RedundantNullCheckMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.temporaryVals.TemporaryVariablesEliminationTransformer
import org.jetbrains.kotlin.codegen.optimization.transformer.CompositeMethodTransformer
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class OptimizationMethodVisitor(
    delegate: MethodVisitor,
    private val mandatoryTransformationsOnly: Boolean,
    private val generationState: GenerationState,
    access: Int,
    name: String,
    desc: String,
    signature: String?,
    exceptions: Array<out String>?
) : TransformationMethodVisitor(delegate, access, name, desc, signature, exceptions) {
    val normalizationMethodTransformer = CompositeMethodTransformer(
        InplaceArgumentsMethodTransformer(),
        FixStackWithLabelNormalizationMethodTransformer(),
        MethodVerifier("AFTER mandatory stack transformations", generationState)
    )

    val optimizationTransformer = CompositeMethodTransformer(
        CapturedVarsOptimizationMethodTransformer(),
        RedundantNullCheckMethodTransformer(generationState),
        RedundantCheckCastEliminationMethodTransformer(),
        ConstantConditionEliminationMethodTransformer(),
        RedundantBoxingMethodTransformer(generationState),
        TemporaryVariablesEliminationTransformer(generationState),
        StackPeepholeOptimizationsTransformer(),
        PopBackwardPropagationTransformer(),
        DeadCodeEliminationMethodTransformer(),
        RedundantGotoMethodTransformer(),
        RedundantNopsCleanupMethodTransformer(),
        NegatedJumpsMethodTransformer(),
        RedundantCheckcastsBeforeAastoreMethodTransformer,
        MethodVerifier("AFTER optimizations", generationState)
    )

    override fun performTransformations(methodNode: MethodNode) {
        normalizationMethodTransformer.transform("fake", methodNode)
        UninitializedStoresProcessor(methodNode).run()

        if (!mandatoryTransformationsOnly && canBeOptimized(methodNode) && !generationState.config.disableOptimization) {
            optimizationTransformer.transform("fake", methodNode)
        }

        DeadCodeEliminationMethodTransformer().transform("fake", methodNode)

        methodNode.prepareForEmitting()
    }

    companion object {
        const val MEMORY_LIMIT_BY_METHOD_MB = 50
        private const val TRY_CATCH_BLOCKS_SOFT_LIMIT = 16

        fun canBeOptimized(node: MethodNode): Boolean {
            if (node.tryCatchBlocks.size > TRY_CATCH_BLOCKS_SOFT_LIMIT) {
                if (getTotalFramesWeight(getTotalTcbSize(node), node) > MEMORY_LIMIT_BY_METHOD_MB)
                    return false
            }
            return getTotalFramesWeight(node.instructions.first.countInsnsWithFramesUntil(null), node) < MEMORY_LIMIT_BY_METHOD_MB
        }

        fun canBeOptimizedUsingSourceInterpreter(node: MethodNode): Boolean {
            val methodSize = node.instructions.first.countInsnsWithFramesUntil(null)
            if (node.tryCatchBlocks.size > TRY_CATCH_BLOCKS_SOFT_LIMIT) {
                if (getTotalFramesWeight(getTotalTcbSize(node) * methodSize, node) > MEMORY_LIMIT_BY_METHOD_MB)
                    return false
            }
            return getTotalFramesWeight(methodSize * methodSize, node) < MEMORY_LIMIT_BY_METHOD_MB
        }

        private fun AbstractInsnNode?.countInsnsWithFramesUntil(end: AbstractInsnNode?): Int {
            var it = this
            var result = 0
            while (it != end && it != null) {
                // FastMethodAnalyzer will reuse the Frame instance when a pseudo-instruction or a NOP is followed
                // by anything other than a jump-targeted label, so those instructions consume no extra memory.
                // To avoid checking all jumps, here we assume all labels are potentially targeted.
                // (Effectively, all of this means that adding line numbers should not inhibit optimization.)
                if ((it.type != AbstractInsnNode.LINE && it.type != AbstractInsnNode.FRAME && it.type != AbstractInsnNode.LABEL &&
                            it.opcode != Opcodes.NOP) || it.next?.type == AbstractInsnNode.LABEL
                ) result++
                it = it.next
            }
            return result
        }

        private fun getTotalFramesWeight(size: Int, node: MethodNode) =
            size.toLong() * (node.maxLocals + node.maxStack) / (1024 * 1024)

        private fun getTotalTcbSize(node: MethodNode) =
            node.tryCatchBlocks.sumOf { it.start.countInsnsWithFramesUntil(it.end) }
    }
}
