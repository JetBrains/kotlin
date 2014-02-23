/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.liveTemplates.macro;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingComponents;
import org.jetbrains.jet.plugin.JetBundle;

public class JetIterableVariableMacro extends BaseJetVariableMacro {

    @Override
    public String getName() {
        return "kotlinIterableVariable";
    }

    @Override
    public String getPresentableName() {
        return JetBundle.message("macro.iterable.variable");
    }

    @Override
    protected boolean isSuitable(
            @NotNull VariableDescriptor variableDescriptor,
            @NotNull JetScope scope,
            @NotNull Project project,
            @NotNull ExpressionTypingComponents components
    ) {
        return components.getForLoopConventionsChecker().isVariableIterable(variableDescriptor, scope);
    }
}
