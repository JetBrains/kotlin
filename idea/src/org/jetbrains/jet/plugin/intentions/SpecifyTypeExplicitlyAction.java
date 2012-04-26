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

package org.jetbrains.jet.plugin.intentions;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.project.AnalyzeSingleFileUtil;
import org.jetbrains.jet.plugin.refactoring.introduceVariable.JetChangePropertyActions;

/**
 * @author Evgeny Gerashchenko
 * @since 4/20/12
 */
public class SpecifyTypeExplicitlyAction extends PsiElementBaseIntentionAction {
    private JetType targetType;

    @NotNull
    @Override
    public String getText() {
        return targetType == null ? JetBundle.message("specify.type.explicitly.remove.action.name") : JetBundle.message("specify.type.explicitly.add.action.name");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("specify.type.explicitly.action.family.name");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        JetProperty property = (JetProperty)element.getParent();
        if (targetType == null) {
            JetChangePropertyActions.removeTypeAnnotation(project, property);
        } else {
            JetChangePropertyActions.addTypeAnnotation(project, property, targetType);
        }
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (!(element.getParent() instanceof JetProperty) || PsiTreeUtil.isAncestor(((JetProperty)element.getParent()).getInitializer(), element, false)) {
            return false;
        }

        JetProperty property = (JetProperty)element.getParent();
        boolean hasTypeSpecified = property.getPropertyTypeRef() != null;
        if (hasTypeSpecified) {
            targetType = null;
        } else {
            BindingContext bindingContext = AnalyzeSingleFileUtil.getContextForSingleFile((JetFile)element.getContainingFile());
            DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property);
            assert descriptor instanceof VariableDescriptor;
            targetType = ((VariableDescriptor)descriptor).getType();
            if (ErrorUtils.isErrorType(targetType)) {
                return false;
            }
        }
        return true;
    }
}
