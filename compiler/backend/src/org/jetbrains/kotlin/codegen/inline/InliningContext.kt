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

    private final Map<Integer, LambdaInfo> expressionMap;
    public final GenerationState state;
    public final NameGenerator nameGenerator;
    public final TypeRemapper typeRemapper;
    public final ReifiedTypeInliner reifiedTypeInliner;
    public final boolean isInliningLambda;
    public final boolean classRegeneration;
    public final Map<String, AnonymousObjectTransformationInfo> internalNameToAnonymousObjectTransformationInfo = new HashMap<>();

    private boolean isContinuation;

    public InliningContext(
            @Nullable InliningContext parent,
            @NotNull Map<Integer, LambdaInfo> expressionMap,
            @NotNull GenerationState state,
            @NotNull NameGenerator nameGenerator,
            @NotNull TypeRemapper typeRemapper,
            @NotNull ReifiedTypeInliner reifiedTypeInliner,
            boolean isInliningLambda,
            boolean classRegeneration
    ) {
        this.parent = parent;
        this.expressionMap = expressionMap;
        this.state = state;
        this.nameGenerator = nameGenerator;
        this.typeRemapper = typeRemapper;
        this.reifiedTypeInliner = reifiedTypeInliner;
        this.isInliningLambda = isInliningLambda;
        this.classRegeneration = classRegeneration;
    }

    @NotNull
    public InliningContext subInline(@NotNull NameGenerator generator) {
        return subInline(generator, Collections.emptyMap(), isInliningLambda);
    }

    @NotNull
    public InliningContext subInlineLambda(@NotNull LambdaInfo lambdaInfo) {
        Map<String, String> map = new HashMap<>();
        map.put(lambdaInfo.getLambdaClassType().getInternalName(), null); //mark lambda inlined
        return subInline(nameGenerator.subGenerator("lambda"), map, true);
    }

    @NotNull
    public InliningContext subInlineWithClassRegeneration(
            @NotNull NameGenerator generator,
            @NotNull Map<String, String> newTypeMappings,
            @NotNull InlineCallSiteInfo callSiteInfo
    ) {
        return new RegeneratedClassContext(
                this, expressionMap, state, generator, TypeRemapper.createFrom(typeRemapper, newTypeMappings),
                reifiedTypeInliner, isInliningLambda, callSiteInfo
        );
    }

    @NotNull
    private InliningContext subInline(
            @NotNull NameGenerator generator, @NotNull Map<String, String> additionalTypeMappings, boolean isInliningLambda
    ) {
        //isInliningLambda && !this.isInliningLambda for root inline lambda
        return new InliningContext(
                this, expressionMap, state, generator,
                TypeRemapper.createFrom(
                        typeRemapper,
                        additionalTypeMappings,
                        //root inline lambda
                        isInliningLambda && !this.isInliningLambda
                ),
                reifiedTypeInliner, isInliningLambda, classRegeneration
        );
    }

    public boolean isRoot() {
        return parent == null;
    }

    @NotNull
    public RootInliningContext getRoot() {
        //noinspection ConstantConditions
        return isRoot() ? (RootInliningContext) this : parent.getRoot();
    }

    @Nullable
    public InliningContext getParent() {
        return parent;
    }

    @NotNull
    public InlineCallSiteInfo getCallSiteInfo() {
        assert parent != null : "At least root context should return proper value";
        return parent.getCallSiteInfo();
    }

    @Nullable
    public AnonymousObjectTransformationInfo findAnonymousObjectTransformationInfo(@NotNull String internalName) {
        if (getRoot().internalNameToAnonymousObjectTransformationInfo.containsKey(internalName)) {
            return getRoot().internalNameToAnonymousObjectTransformationInfo.get(internalName);
        }

        return null;
    }

    public boolean isContinuation() {
        return isContinuation;
    }

    public void setContinuation(boolean continuation) {
        isContinuation = continuation;
    }
}
