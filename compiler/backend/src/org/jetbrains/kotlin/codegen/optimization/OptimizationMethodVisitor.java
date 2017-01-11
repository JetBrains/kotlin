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

package org.jetbrains.kotlin.codegen.optimization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.TransformationMethodVisitor;
import org.jetbrains.kotlin.codegen.optimization.boxing.RedundantBoxingMethodTransformer;
import org.jetbrains.kotlin.codegen.optimization.boxing.RedundantCoercionToUnitTransformer;
import org.jetbrains.kotlin.codegen.optimization.boxing.RedundantNullCheckMethodTransformer;
import org.jetbrains.kotlin.codegen.optimization.common.UtilKt;
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;

public class OptimizationMethodVisitor extends TransformationMethodVisitor {
    private static final int MEMORY_LIMIT_BY_METHOD_MB = 50;

    private static final MethodTransformer MANDATORY_METHOD_TRANSFORMER = new FixStackWithLabelNormalizationMethodTransformer();

    private static final MethodTransformer[] OPTIMIZATION_TRANSFORMERS = new MethodTransformer[] {
            new RedundantNullCheckMethodTransformer(),
            new RedundantBoxingMethodTransformer(),
            new RedundantCoercionToUnitTransformer(),
            new DeadCodeEliminationMethodTransformer(),
            new RedundantGotoMethodTransformer(),
            new RedundantNopsCleanupMethodTransformer()
    };

    private final boolean disableOptimization;

    public OptimizationMethodVisitor(
            @NotNull MethodVisitor delegate,
            boolean disableOptimization,
            int access,
            @NotNull String name,
            @NotNull String desc,
            @Nullable String signature,
            @Nullable String[] exceptions
    ) {
        super(delegate, access, name, desc, signature, exceptions);
        this.disableOptimization = disableOptimization;
    }

    @Override
    protected void performTransformations(@NotNull MethodNode methodNode) {
        MANDATORY_METHOD_TRANSFORMER.transform("fake", methodNode);
        if (canBeOptimized(methodNode) && !disableOptimization) {
            for (MethodTransformer transformer : OPTIMIZATION_TRANSFORMERS) {
                transformer.transform("fake", methodNode);
            }
        }
        UtilKt.prepareForEmitting(methodNode);
    }

    private static boolean canBeOptimized(@NotNull MethodNode node) {
        int totalFramesSizeMb = node.instructions.size() * (node.maxLocals + node.maxStack) / (1024 * 1024);
        return totalFramesSizeMb < MEMORY_LIMIT_BY_METHOD_MB;
    }
}
