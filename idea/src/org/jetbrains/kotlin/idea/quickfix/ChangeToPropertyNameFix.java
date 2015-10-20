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
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.psi.*;

public class ChangeToPropertyNameFix extends KotlinQuickFixAction<KtSimpleNameExpression> {
    public ChangeToPropertyNameFix(@NotNull KtSimpleNameExpression element) {
        super(element);
    }


    private String getBackingFieldName() {
        return getElement().getText();
    }

    private String getPropertyName() {
        return getBackingFieldName().replaceFirst("\\$", "");
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("change.to.property.name.action", getBackingFieldName(), getPropertyName());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.to.property.name.family.name");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        KtSimpleNameExpression propertyName = (KtSimpleNameExpression) KtPsiFactoryKt.KtPsiFactory(file).createExpression(getPropertyName());
        getElement().replace(propertyName);
    }

    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Override
            public KotlinQuickFixAction<KtSimpleNameExpression> createAction(Diagnostic diagnostic) {
                KtSimpleNameExpression expression = QuickFixUtil.getParentElementOfType(diagnostic, KtSimpleNameExpression.class);
                if (expression == null) {
                    PsiElement element = diagnostic.getPsiElement();
                    if (element instanceof KtQualifiedExpression && ((KtQualifiedExpression) element).getReceiverExpression() instanceof KtThisExpression) {
                        KtExpression selector = ((KtQualifiedExpression) element).getSelectorExpression();
                        if (selector instanceof KtSimpleNameExpression) {
                            expression = (KtSimpleNameExpression) selector;
                        }
                    }
                }
                if (expression == null) return null;
                return new ChangeToPropertyNameFix(expression);
            }
        };
    }
}
