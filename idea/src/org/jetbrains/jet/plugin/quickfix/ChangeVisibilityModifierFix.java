/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetModifierListOwner;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.project.AnalyzeSingleFileUtil;

/**
 * @author svtk
 */
public class ChangeVisibilityModifierFix extends JetIntentionAction<JetModifierListOwner> {
    public ChangeVisibilityModifierFix(@NotNull JetModifierListOwner element) {
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
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (!(file instanceof JetFile)) return;
        JetKeywordToken modifier = findVisibilityChangeTo((JetFile)file);
        JetToken[] modifiersThanCanBeReplaced = new JetKeywordToken[] { JetTokens.PUBLIC_KEYWORD, JetTokens.PRIVATE_KEYWORD, JetTokens.PROTECTED_KEYWORD, JetTokens.INTERNAL_KEYWORD };
        element.replace(AddModifierFix.addModifier(element, modifier, modifiersThanCanBeReplaced, project, true));
    }

    private JetKeywordToken findVisibilityChangeTo(JetFile file) {
        BindingContext bindingContext = AnalyzeSingleFileUtil.getContextForSingleFile(file);
        DeclarationDescriptor descriptor;
        if (element instanceof JetParameter) {
            descriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, element);
        }
        else {
            descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
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
        JetKeywordToken modifier = null;
        if (maxVisibility == Visibilities.PUBLIC) {
            modifier = JetTokens.PUBLIC_KEYWORD;
        }
        else if (maxVisibility == Visibilities.PROTECTED) {
            modifier = JetTokens.PROTECTED_KEYWORD;
        }
        else if (maxVisibility == Visibilities.INTERNAL) {
            modifier = JetTokens.INTERNAL_KEYWORD;
        }
        return modifier;
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetModifierListOwner> createAction(Diagnostic diagnostic) {
                PsiElement element = diagnostic.getPsiElement();
                if (!(element instanceof JetModifierListOwner)) return null;
                return new ChangeVisibilityModifierFix((JetModifierListOwner)element);
            }
        };
    }

}
