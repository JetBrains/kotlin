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
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetFunctionPlatformDescriptorImpl;

import java.util.List;

public class RemoveFunctionParametersFix extends ChangeFunctionSignatureFix {
    private final ValueParameterDescriptor parameterToRemove;

    public RemoveFunctionParametersFix(
            @NotNull PsiElement element,
            @NotNull PsiElement context,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull ValueParameterDescriptor parameterToRemove
    ) {
        super(element, context, functionDescriptor);
        this.parameterToRemove = parameterToRemove;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("remove.parameter", parameterToRemove.getName().getName());
    }

    @Override
    protected void invoke(@NotNull Project project, Editor editor, JetFile file) {
        JetFunctionPlatformDescriptorImpl platformDescriptor = new JetFunctionPlatformDescriptorImpl(functionDescriptor, element);
        List<ValueParameterDescriptor> parameters = functionDescriptor.getValueParameters();
        platformDescriptor.removeParameter(parameters.indexOf(parameterToRemove));
        showDialog(project, platformDescriptor);
    }
}
