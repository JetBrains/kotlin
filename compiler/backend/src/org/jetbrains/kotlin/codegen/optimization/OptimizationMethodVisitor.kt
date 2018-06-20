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
import org.jetbrains.kotlin.codegen.optimization.boxing.PopBackwardPropagationTransformer
import org.jetbrains.kotlin.codegen.optimization.boxing.RedundantBoxingMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.boxing.StackPeepholeOptimizationsTransformer
import org.jetbrains.kotlin.codegen.optimization.common.prepareForEmitting
import org.jetbrains.kotlin.codegen.optimization.nullCheck.RedundantNullCheckMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.transformer.CompositeMethodTransformer
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class OptimizationMethodVisitor(
    delegate: MethodVisitor,
    private val generationState: GenerationState,
    access: Int,
    name: String,
    desc: String,
    signature: String?,
    exceptions: Array<String>?
) : TransformationMethodVisitor(delegate, access, name, desc, signature, exceptions) {
    private val constructorCallNormalizationTransformer =
        UninitializedStoresMethodTransformer(generationState.constructorCallNormalizationMode)

    val normalizationMethodTransformer = CompositeMethodTransformer(
        FixStackWithLabelNormalizationMethodTransformer(),
        MethodVerifier("AFTER mandatory stack transformations")
    )

    val optimizationTransformer = CompositeMethodTransformer(
        CapturedVarsOptimizationMethodTransformer(),
        RedundantNullCheckMethodTransformer(generationState),
        RedundantCheckCastEliminationMethodTransformer(),
        ConstantConditionEliminationMethodTransformer(),
        RedundantBoxingMethodTransformer(generationState),
        StackPeepholeOptimizationsTransformer(),
        PopBackwardPropagationTransformer(),
        DeadCodeEliminationMethodTransformer(),
        RedundantGotoMethodTransformer(),
        RedundantNopsCleanupMethodTransformer(),
        MethodVerifier("AFTER optimizations")
    )

    override fun performTransformations(methodNode: MethodNode) {
        normalizationMethodTransformer.transform("fake", methodNode)
        constructorCallNormalizationTransformer.transform("fake", methodNode)

        if (canBeOptimized(methodNode) && !generationState.disableOptimization) {
            optimizationTransformer.transform("fake", methodNode)
        }

        methodNode.prepareForEmitting()
    }

    companion object {
        private val MEMORY_LIMIT_BY_METHOD_MB = 50

        fun canBeOptimized(node: MethodNode): Boolean {
            val totalFramesSizeMb = node.instructions.size() * (node.maxLocals + node.maxStack) / (1024 * 1024)
            return totalFramesSizeMb < MEMORY_LIMIT_BY_METHOD_MB
        }

        fun canBeOptimizedUsingSourceInterpreter(node: MethodNode): Boolean {
            val frameSize = node.maxLocals + node.maxStack
            val methodSize = node.instructions.size().toLong()
            val totalFramesSizeMb = methodSize * methodSize * frameSize / (1024 * 1024)
            return totalFramesSizeMb < MEMORY_LIMIT_BY_METHOD_MB
        }
    }
}
