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
import org.jetbrains.kotlin.codegen.context.CodegenContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.psi.KtElement;

import java.util.Map;

public class RootInliningContext extends InliningContext {
    public final CodegenContext startContext;
    private final InlineCallSiteInfo inlineCallSiteInfo;
    public final TypeParameterMappings typeParameterMappings;
    public final boolean isDefaultCompilation;
    public final KtElement callElement;

    public RootInliningContext(
            @NotNull Map<Integer, LambdaInfo> map,
            @NotNull GenerationState state,
            @NotNull NameGenerator nameGenerator,
            @NotNull CodegenContext startContext,
            @NotNull KtElement callElement,
            @NotNull InlineCallSiteInfo classNameToInline,
            @NotNull ReifiedTypeInliner inliner,
            @Nullable TypeParameterMappings typeParameterMappings,
            boolean isDefaultCompilation
    ) {
        super(null, map, state, nameGenerator, TypeRemapper.createRoot(typeParameterMappings), inliner, false, false);
        this.callElement = callElement;
        this.startContext = startContext;
        this.inlineCallSiteInfo = classNameToInline;
        this.typeParameterMappings = typeParameterMappings;
        this.isDefaultCompilation = isDefaultCompilation;
    }

    @Override
    public InlineCallSiteInfo getCallSiteInfo() {
        return inlineCallSiteInfo;
    }
}
