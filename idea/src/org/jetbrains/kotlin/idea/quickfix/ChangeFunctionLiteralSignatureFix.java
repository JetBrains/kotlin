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

package org.jetbrains.kotlin.idea.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.core.CollectingNameValidator;
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetFunctionLiteral;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.types.JetType;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.idea.refactoring.changeSignature.ChangeSignaturePackage.runChangeSignature;

public class ChangeFunctionLiteralSignatureFix extends ChangeFunctionSignatureFix {
    private final List<JetType> parameterTypes;

    public ChangeFunctionLiteralSignatureFix(
            @NotNull JetFunctionLiteral functionLiteral,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull List<JetType> parameterTypes) {
        super(functionLiteral, functionDescriptor);
        this.parameterTypes = parameterTypes;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("change.function.literal.signature");
    }

    @Override
    protected void invoke(@NotNull Project project, Editor editor, JetFile file) {
        BindingContext bindingContext = ResolvePackage.analyzeFully(file);
        runChangeSignature(project, functionDescriptor, new JetChangeSignatureConfiguration() {
            @NotNull
            @Override
            public JetMethodDescriptor configure(@NotNull JetMethodDescriptor originalDescriptor, @NotNull BindingContext bindingContext) {
                return ChangeSignaturePackage.modify(
                        originalDescriptor,
                        new Function1<JetMutableMethodDescriptor, Unit>() {
                            @Override
                            public Unit invoke(JetMutableMethodDescriptor descriptor) {
                                CollectingNameValidator validator = new CollectingNameValidator();
                                descriptor.clearNonReceiverParameters();
                                for (JetType type : parameterTypes) {
                                    String name = KotlinNameSuggester.INSTANCE$.suggestNamesByType(type, validator, "param")[0];
                                    descriptor.addParameter(
                                            new JetParameterInfo(functionDescriptor, -1, name, type, null, null, JetValVar.None, null)
                                    );
                                }
                                return null;
                            }
                        }
                );
            }

            @Override
            public boolean performSilently(@NotNull Collection<? extends PsiElement> elements) {
                return false;
            }

            @Override
            public boolean forcePerformForSelectedFunctionOnly() {
                return false;
            }
        }, bindingContext, context, getText());
    }
}
