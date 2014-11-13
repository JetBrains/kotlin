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
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetElement;

import java.util.Collections;
import java.util.Map;

public class RootInliningContext extends InliningContext {

    public final CodegenContext startContext;

    private final String classNameToInline;

    public final JetElement callElement;

    public RootInliningContext(
            @NotNull Map<Integer, LambdaInfo> map,
            @NotNull GenerationState state,
            @NotNull NameGenerator nameGenerator,
            @NotNull CodegenContext startContext,
            @NotNull JetElement callElement,
            @NotNull String classNameToInline,
            @NotNull ReifiedTypeInliner inliner
    ) {
        super(null, map, state, nameGenerator, Collections.<String, String>emptyMap(), inliner, false, false);
        this.callElement = callElement;
        this.startContext = startContext;
        this.classNameToInline = classNameToInline;
    }

    @Override
    @NotNull
    public String getClassNameToInline() {
        return classNameToInline;
    }
}
