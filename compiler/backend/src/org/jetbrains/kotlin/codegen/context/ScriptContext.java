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

package org.jetbrains.kotlin.codegen.context;

import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.FieldInfo;
import org.jetbrains.kotlin.codegen.OwnerKind;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ScriptDescriptor;
import org.jetbrains.kotlin.psi.KtAnonymousInitializer;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtScript;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;

import java.util.List;

public class ScriptContext extends ClassContext {
    private final ScriptDescriptor scriptDescriptor;
    private final List<ScriptDescriptor> earlierScripts;
    private final KtExpression lastStatement;

    public ScriptContext(
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull ScriptDescriptor scriptDescriptor,
            @NotNull List<ScriptDescriptor> earlierScripts,
            @NotNull ClassDescriptor contextDescriptor,
            @Nullable CodegenContext parentContext
    ) {
        super(typeMapper, contextDescriptor, OwnerKind.IMPLEMENTATION, parentContext, null);
        this.scriptDescriptor = scriptDescriptor;
        this.earlierScripts = earlierScripts;
        KtScript script = (KtScript) DescriptorToSourceUtils.getSourceFromDescriptor(scriptDescriptor);
        assert script != null : "Declaration should be present for script: " + scriptDescriptor;
        KtDeclaration lastDeclaration = CollectionsKt.lastOrNull(script.getDeclarations());
        if (lastDeclaration instanceof KtAnonymousInitializer) {
            this.lastStatement = ((KtAnonymousInitializer) lastDeclaration).getBody();
        }
        else {
            this.lastStatement = null;
        }
    }

    @NotNull
    public ScriptDescriptor getScriptDescriptor() {
        return scriptDescriptor;
    }

    @NotNull
    public FieldInfo getResultFieldInfo() {
        assert getState().getReplSpecific().getShouldGenerateScriptResultValue() : "Should not be called unless 'scriptResultFieldName' is set";
        GenerationState state = getState();
        String scriptResultFieldName = state.getReplSpecific().getScriptResultFieldName();
        assert scriptResultFieldName != null;
        return FieldInfo.createForHiddenField(state.getTypeMapper().mapClass(scriptDescriptor), AsmTypes.OBJECT_TYPE, scriptResultFieldName);
    }

    @NotNull
    public List<ScriptDescriptor> getEarlierScripts() {
        return earlierScripts;
    }

    @NotNull
    public String getScriptFieldName(@NotNull ScriptDescriptor scriptDescriptor) {
        int index = earlierScripts.indexOf(scriptDescriptor);
        if (index < 0) {
            throw new IllegalStateException("Unregistered script: " + scriptDescriptor);
        }
        return "script$" + (index + 1);
    }

    @Nullable
    public KtExpression getLastStatement() {
        return lastStatement;
    }

    @Override
    public String toString() {
        return "Script: " + getContextDescriptor().getName().asString();
    }
}
