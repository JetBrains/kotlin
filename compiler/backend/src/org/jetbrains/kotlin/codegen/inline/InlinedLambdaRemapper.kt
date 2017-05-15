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

package org.jetbrains.kotlin.codegen.inline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode;

import java.util.Collection;

public class InlinedLambdaRemapper extends FieldRemapper {
    public InlinedLambdaRemapper(
            @NotNull String lambdaInternalName,
            @NotNull FieldRemapper parent,
            @NotNull Parameters methodParams
    ) {
        super(lambdaInternalName, parent, methodParams);
    }

    @Override
    public boolean canProcess(@NotNull String fieldOwner, @NotNull String fieldName, boolean isFolding) {
        return isFolding && super.canProcess(fieldOwner, fieldName, true);
    }

    @Override
    @Nullable
    public CapturedParamInfo findField(@NotNull FieldInsnNode fieldInsnNode, @NotNull Collection<CapturedParamInfo> captured) {
        return parent.findField(fieldInsnNode, captured);
    }

    @Override
    public boolean isInsideInliningLambda() {
        return true;
    }

    @Nullable
    @Override
    public StackValue getFieldForInline(@NotNull FieldInsnNode node, @Nullable StackValue prefix) {
        if (parent.isRoot()) {
            return super.getFieldForInline(node, prefix);
        }
        return parent.getFieldForInline(node, prefix);
    }
}
