/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.inline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.tree.FieldInsnNode;

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
    public void addCapturedFields(
            LambdaInfo lambdaInfo, ParametersBuilder builder
    ) {
        parent.addCapturedFields(lambdaInfo, builder);
    }


    @Override
    @Nullable
    public CapturedParamInfo findField(
            @NotNull FieldInsnNode fieldInsnNode,
            @NotNull Collection<CapturedParamInfo> captured
    ) {
        return parent.findField(fieldInsnNode, captured);
    }

    @Override
    public FieldRemapper getParent() {
        return parent.getParent();
    }

    @Override
    public boolean isRoot() {
        return parent.isRoot();
    }
}
