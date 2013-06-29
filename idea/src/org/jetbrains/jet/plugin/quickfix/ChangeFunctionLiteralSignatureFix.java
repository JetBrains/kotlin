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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetFunctionLiteral;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester;
import org.jetbrains.jet.plugin.refactoring.JetNameValidator;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetFunctionPlatformDescriptorImpl;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetParameterInfo;

import java.util.List;

public class ChangeFunctionLiteralSignatureFix extends ChangeFunctionSignatureFix {
    private final List<JetType> parameterTypes;

    public ChangeFunctionLiteralSignatureFix(
            @NotNull JetFunctionLiteral functionLiteral,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull List<JetType> parameterTypes) {
        super(functionLiteral, functionLiteral, functionDescriptor);
        this.parameterTypes = parameterTypes;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("change.function.literal.signature");
    }

    @Override
    protected void invoke(@NotNull Project project, Editor editor, JetFile file) {
        JetFunctionPlatformDescriptorImpl platformDescriptor = new JetFunctionPlatformDescriptorImpl(functionDescriptor, element);
        JetNameValidator validator = JetNameValidator.getCollectingValidator(project);
        platformDescriptor.clearParameters();

        for (JetType type : parameterTypes) {
            String name = JetNameSuggester.suggestNames(type, validator, "param")[0];
            platformDescriptor.addParameter(new JetParameterInfo(name, type));
        }

        showDialog(project, platformDescriptor);
    }
}
