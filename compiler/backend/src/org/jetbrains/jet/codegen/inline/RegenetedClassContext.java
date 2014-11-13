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
import org.jetbrains.jet.codegen.state.GenerationState;

import java.util.Map;

public class RegenetedClassContext extends InliningContext {

    @NotNull
    private final AnonymousObjectGeneration anonymousObjectGeneration;

    protected RegenetedClassContext(
            @Nullable InliningContext parent,
            @NotNull Map<Integer, LambdaInfo> map,
            @NotNull GenerationState state,
            @NotNull NameGenerator nameGenerator,
            @NotNull Map<String, String> typeMapping,
            @NotNull ReifiedTypeInliner reifiedTypeInliner,
            boolean isInliningLambda,
            @NotNull AnonymousObjectGeneration anonymousObjectGeneration
    ) {
        super(parent, map, state, nameGenerator, typeMapping, reifiedTypeInliner, isInliningLambda, true);
        this.anonymousObjectGeneration = anonymousObjectGeneration;
    }

    @Override
    public String getClassNameToInline() {
        return anonymousObjectGeneration.getOwnerInternalName();
    }
}
