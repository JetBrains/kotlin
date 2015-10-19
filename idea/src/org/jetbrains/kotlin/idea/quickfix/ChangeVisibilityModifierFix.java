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
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.Visibilities;
import org.jetbrains.kotlin.descriptors.Visibility;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.core.DescriptorUtilsKt;
import org.jetbrains.kotlin.idea.core.PsiModificationUtilsKt;
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetParameter;
import org.jetbrains.kotlin.resolve.BindingContext;

public class ChangeVisibilityModifierFix extends KotlinQuickFixAction<JetDeclaration> {
    public ChangeVisibilityModifierFix(@NotNull JetDeclaration element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("change.visibility.modifier");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.visibility.modifier");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!(file instanceof JetFile)) return false;
        return super.isAvailable(project, editor, file) && (findVisibilityChangeTo((JetFile)file) != null);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        JetModifierKeywordToken modifier = findVisibilityChangeTo(file);
        assert modifier != null;
        PsiModificationUtilsKt.setVisibility(getElement(), modifier);
    }

    @Nullable
    private JetModifierKeywordToken findVisibilityChangeTo(JetFile file) {
        BindingContext bindingContext = ResolutionUtils.analyze(getElement());
        DeclarationDescriptor descriptor;
        if (getElement() instanceof JetParameter) {
            descriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, getElement());
        }
        else {
            descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, getElement());
        }
        if (!(descriptor instanceof CallableMemberDescriptor)) return null;

        CallableMemberDescriptor memberDescriptor = (CallableMemberDescriptor)descriptor;
        Visibility maxVisibility = null;
        for (CallableMemberDescriptor overriddenDescriptor : memberDescriptor.getOverriddenDescriptors()) {
            Visibility overriddenDescriptorVisibility = overriddenDescriptor.getVisibility();
            if (maxVisibility == null) {
                maxVisibility = overriddenDescriptorVisibility;
                continue;
            }
            Integer compare = Visibilities.compare(maxVisibility, overriddenDescriptorVisibility);
            if (compare == null) {
                maxVisibility = Visibilities.PUBLIC;
            }
            else if (compare < 0) {
                maxVisibility = overriddenDescriptorVisibility;
            }
        }
        if (maxVisibility == memberDescriptor.getVisibility()) {
            return null;
        }

        if (maxVisibility == null) {
            return null;
        }

        return DescriptorUtilsKt.toKeywordToken(maxVisibility);
    }

    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Override
            public KotlinQuickFixAction<JetDeclaration> createAction(Diagnostic diagnostic) {
                PsiElement element = diagnostic.getPsiElement();
                if (!(element instanceof JetDeclaration)) return null;
                return new ChangeVisibilityModifierFix((JetDeclaration)element);
            }
        };
    }

}
