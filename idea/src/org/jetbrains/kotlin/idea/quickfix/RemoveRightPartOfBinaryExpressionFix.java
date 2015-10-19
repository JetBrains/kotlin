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

public class RemoveRightPartOfBinaryExpressionFix<T extends KtExpression> extends KotlinQuickFixAction<T> implements CleanupFix {
    private final String message;
    
    public RemoveRightPartOfBinaryExpressionFix(@NotNull T element, String message) {
        super(element);
        this.message = message;
    }

    @Override
    public String getText() {
        return message;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.right.part.of.binary.expression");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        invoke();
    }

    @NotNull
    public KtExpression invoke() throws IncorrectOperationException {
        KtExpression newExpression = null;

        if (getElement() instanceof KtBinaryExpression) {
            //noinspection ConstantConditions
            newExpression = (KtExpression) getElement().replace(((KtBinaryExpression) getElement().copy()).getLeft());
        }
        else if (getElement() instanceof KtBinaryExpressionWithTypeRHS) {
            newExpression = (KtExpression) getElement().replace(((KtBinaryExpressionWithTypeRHS) getElement().copy()).getLeft());
        }

        PsiElement parent = newExpression != null ? newExpression.getParent() : null;
        if (parent instanceof KtParenthesizedExpression && KtPsiUtil.areParenthesesUseless((KtParenthesizedExpression) parent)) {
            newExpression = (KtExpression) parent.replace(newExpression);
        }

        return newExpression;
    }

    public static JetSingleIntentionActionFactory createRemoveTypeFromBinaryExpressionFactory(final String message) {
        return new JetSingleIntentionActionFactory() {
            @Override
            public KotlinQuickFixAction<KtBinaryExpressionWithTypeRHS> createAction(Diagnostic diagnostic) {
                KtBinaryExpressionWithTypeRHS expression = QuickFixUtil.getParentElementOfType(diagnostic, KtBinaryExpressionWithTypeRHS.class);
                if (expression == null) return null;
                return new RemoveRightPartOfBinaryExpressionFix<KtBinaryExpressionWithTypeRHS>(expression, message);
            }
        };
    }

    public static JetSingleIntentionActionFactory createRemoveElvisOperatorFactory() {
        return new JetSingleIntentionActionFactory() {
            @Override
            public KotlinQuickFixAction<KtBinaryExpression> createAction(Diagnostic diagnostic) {
                KtBinaryExpression expression = (KtBinaryExpression) diagnostic.getPsiElement();
                return new RemoveRightPartOfBinaryExpressionFix<KtBinaryExpression>(expression, JetBundle.message("remove.elvis.operator"));
            }
        };
    }
}

