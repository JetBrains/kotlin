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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetChangeSignatureConfiguration;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetChangeSignatureData;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.plugin.refactoring.changeSignature.ChangeSignaturePackage.runChangeSignature;

public class RemoveFunctionParametersFix extends ChangeFunctionSignatureFix {
    private final ValueParameterDescriptor parameterToRemove;

    public RemoveFunctionParametersFix(
            @NotNull PsiElement context,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull ValueParameterDescriptor parameterToRemove
    ) {
        super(context, functionDescriptor);
        this.parameterToRemove = parameterToRemove;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("remove.parameter", parameterToRemove.getName().asString());
    }

    @Override
    protected void invoke(@NotNull Project project, Editor editor, JetFile file) {
        BindingContext bindingContext = ResolvePackage.analyzeFully(file);
        runChangeSignature(project, functionDescriptor, new JetChangeSignatureConfiguration() {
            @Override
            public void configure(
                    @NotNull JetChangeSignatureData changeSignatureData, @NotNull BindingContext bindingContext
            ) {
                List<ValueParameterDescriptor> parameters = functionDescriptor.getValueParameters();
                changeSignatureData.removeParameter(parameters.indexOf(parameterToRemove));
            }

            @Override
            public boolean performSilently(@NotNull Collection<? extends PsiElement> elements) {
                return false;
            }
        }, bindingContext, context, getText());
    }
}
