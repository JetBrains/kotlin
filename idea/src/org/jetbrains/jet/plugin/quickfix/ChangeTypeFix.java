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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;

public class ChangeTypeFix extends JetIntentionAction<JetTypeReference> {
    private final JetType changeToType;
    private final boolean isProperty;

    public ChangeTypeFix(@NotNull JetTypeReference element, @NotNull JetType changeToType, boolean isProperty) {
        super(element);
        this.changeToType = changeToType;
        this.isProperty = isProperty;
    }

    @NotNull
    @Override
    public String getText() {
        return isProperty
               ? JetBundle.message("change.type.to.match.overridden.property", element.getText(), changeToType.toString())
               : JetBundle.message("change.type.to.match.overridden.method", element.getText(), changeToType.toString());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.type.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        PsiElement newTypeElement = JetPsiFactory.createType(project, changeToType.toString());
        element.replace(newTypeElement);
    }

    public static JetIntentionActionFactory createChangeTypeToMatchOverriddenPropertyOrFunctionFactory(final boolean isProperty) {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetFile file = (JetFile) diagnostic.getPsiFile();
                ResolveSession resolveSession = WholeProjectAnalyzerFacade.getLazyResolveSessionForFile(file);

                JetDeclaration functionOrProperty;
                JetTypeReference typeRef;

                if (isProperty) {
                    JetProperty property = QuickFixUtil.getParentElementOfType(diagnostic, JetProperty.class);
                    if (property == null) return null;
                    typeRef = property.getTypeRef();
                    functionOrProperty = property;
                } else {
                    JetFunction function = QuickFixUtil.getParentElementOfType(diagnostic, JetFunction.class);
                    if (function == null) return null;
                    typeRef = function.getReturnTypeRef();
                    functionOrProperty = function;
                }

                DeclarationDescriptor descriptor = resolveSession.resolveToDescriptor(functionOrProperty);
                if (typeRef == null || !(descriptor instanceof CallableMemberDescriptor)) return null;
                CallableMemberDescriptor callable = (CallableMemberDescriptor) descriptor;

                JetType changeToType = null;
                for (CallableMemberDescriptor overriddenDescriptor : callable.getOverriddenDescriptors()) {
                    if (changeToType == null) {
                        changeToType = overriddenDescriptor.getReturnType();
                    } else if (!changeToType.equals(overriddenDescriptor.getReturnType())) {
                        return null;
                    }
                }
                if (changeToType == null) {
                    return null;
                }

                return new ChangeTypeFix(typeRef, changeToType, isProperty);
            }
        };
    }
}
