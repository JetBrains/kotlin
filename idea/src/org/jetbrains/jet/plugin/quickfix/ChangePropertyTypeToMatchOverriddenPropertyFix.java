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
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManagerUtil;
import org.jetbrains.jet.plugin.intentions.SpecifyTypeExplicitlyAction;

public class ChangePropertyTypeToMatchOverriddenPropertyFix extends JetIntentionAction<JetProperty> {
    private JetType matchingType;

    public ChangePropertyTypeToMatchOverriddenPropertyFix(@NotNull JetProperty property) {
        super(property);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }

        BindingContext context = KotlinCacheManagerUtil.getDeclarationsBindingContext((JetFile) file);
        matchingType = QuickFixUtil.findLowerBoundOfOverriddenCallablesReturnTypes(context, element);
        return matchingType != null;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("change.property.type.to.match.overridden.property", matchingType);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.type.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetTypeReference typeReference = element.getTypeRef();
        if (typeReference == null) {
            SpecifyTypeExplicitlyAction.addTypeAnnotation(project, editor, element, matchingType);
        }
        else {
            PsiElement newType = JetPsiFactory.createType(project, matchingType.toString());
            typeReference.replace(newType);
        }
    }

    @NotNull
    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetProperty property = QuickFixUtil.getParentElementOfType(diagnostic, JetProperty.class);
                return property == null ? null : new ChangePropertyTypeToMatchOverriddenPropertyFix(property);
            }
        };
    }
}
