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
import org.jetbrains.kotlin.codegen.state.GenerationState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InliningContext {

    @Nullable
    private final InliningContext parent;

    public final Map<Integer, LambdaInfo> expressionMap;

    public final GenerationState state;

    public final NameGenerator nameGenerator;

    public final Map<String, String> typeMapping;

    public final ReifiedTypeInliner reifedTypeInliner;

    public final boolean isInliningLambda;

    public final boolean classRegeneration;

    protected InliningContext(
            @Nullable InliningContext parent,
            @NotNull Map<Integer, LambdaInfo> map,
            @NotNull GenerationState state,
            @NotNull NameGenerator nameGenerator,
            @NotNull Map<String, String> typeMapping,
            @NotNull ReifiedTypeInliner reifedTypeInliner,
            boolean isInliningLambda,
            boolean classRegeneration
    ) {
        this.parent = parent;
        expressionMap = map;
        this.state = state;
        this.nameGenerator = nameGenerator;
        this.typeMapping = typeMapping;
        this.reifedTypeInliner = reifedTypeInliner;
        this.isInliningLambda = isInliningLambda;
        this.classRegeneration = classRegeneration;
    }

    public InliningContext subInline(NameGenerator generator) {
        return subInline(generator, Collections.<String, String>emptyMap());
    }

    public InliningContext subInlineLambda(LambdaInfo lambdaInfo) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(lambdaInfo.getLambdaClassType().getInternalName(), null); //mark lambda inlined
        return subInline(nameGenerator.subGenerator("lambda"), map, true);
    }

    public InliningContext subInline(NameGenerator generator, Map<String, String> additionalTypeMappings) {
        return subInline(generator, additionalTypeMappings, isInliningLambda);
    }

    public InliningContext subInlineWithClassRegeneration(@NotNull NameGenerator generator,
            @NotNull Map<String, String> additionalTypeMappings,
            @NotNull AnonymousObjectGeneration anonymousObjectGeneration
    ) {
        Map<String, String> newTypeMappings = new HashMap<String, String>(typeMapping);
        newTypeMappings.putAll(additionalTypeMappings);
        return new RegeneratedClassContext(this, expressionMap, state, generator,
                                           newTypeMappings, reifedTypeInliner, isInliningLambda, anonymousObjectGeneration);
    }

    public InliningContext subInline(NameGenerator generator, Map<String, String> additionalTypeMappings, boolean isInliningLambda) {
        return subInline(generator, additionalTypeMappings, isInliningLambda, classRegeneration);
    }

    private InliningContext subInline(
            NameGenerator generator,
            Map<String, String> additionalTypeMappings,
            boolean isInliningLambda,
            boolean isRegeneration
    ) {
        Map<String, String> newTypeMappings = new HashMap<String, String>(typeMapping);
        newTypeMappings.putAll(additionalTypeMappings);
        return new InliningContext(this, expressionMap, state, generator,
                                   newTypeMappings, reifedTypeInliner, isInliningLambda, isRegeneration);
    }

    public boolean isRoot() {
        return parent == null;
    }

    @NotNull
    public RootInliningContext getRoot() {
        if (isRoot()) {
            return (RootInliningContext) this;
        }
        else {
            return parent.getRoot();
        }
    }

    @Nullable
    public InliningContext getParent() {
        return parent;
    }

    public boolean isInliningLambdaRootContext() {
        //noinspection ConstantConditions
        return isInliningLambda && !getParent().isInliningLambda;
    }

    public String getClassNameToInline() {
        assert parent != null : "At least root context should return proper value";
        return parent.getClassNameToInline();
    }
}
