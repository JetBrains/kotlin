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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.psi.*;

public class RemoveRightPartOfBinaryExpressionFix<T extends JetExpression> extends JetIntentionAction<T> implements CleanupFix {
    private final String message;
    
    public RemoveRightPartOfBinaryExpressionFix(@NotNull T element, String message) {
        super(element);
        this.message = message;
    }

    public String getText() {
        return message;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.right.part.of.binary.expression");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        invoke();
    }

    @NotNull
    public JetExpression invoke() throws IncorrectOperationException {
        JetExpression newExpression = null;

        if (element instanceof JetBinaryExpression) {
            //noinspection ConstantConditions
            newExpression = (JetExpression) element.replace(((JetBinaryExpression) element.copy()).getLeft());
        }
        else if (element instanceof JetBinaryExpressionWithTypeRHS) {
            newExpression = (JetExpression) element.replace(((JetBinaryExpressionWithTypeRHS) element.copy()).getLeft());
        }

        PsiElement parent = newExpression != null ? newExpression.getParent() : null;
        if (parent instanceof JetParenthesizedExpression && JetPsiUtil.areParenthesesUseless((JetParenthesizedExpression) parent)) {
            newExpression = (JetExpression) parent.replace(newExpression);
        }

        return newExpression;
    }

    public static JetSingleIntentionActionFactory createRemoveTypeFromBinaryExpressionFactory(final String message) {
        return new JetSingleIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetBinaryExpressionWithTypeRHS> createAction(Diagnostic diagnostic) {
                JetBinaryExpressionWithTypeRHS expression = QuickFixUtil.getParentElementOfType(diagnostic, JetBinaryExpressionWithTypeRHS.class);
                if (expression == null) return null;
                return new RemoveRightPartOfBinaryExpressionFix<JetBinaryExpressionWithTypeRHS>(expression, message);
            }
        };
    }

    public static JetSingleIntentionActionFactory createRemoveElvisOperatorFactory() {
        return new JetSingleIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetBinaryExpression> createAction(Diagnostic diagnostic) {
                JetBinaryExpression expression = (JetBinaryExpression) diagnostic.getPsiElement();
                return new RemoveRightPartOfBinaryExpressionFix<JetBinaryExpression>(expression, JetBundle.message("remove.elvis.operator"));
            }
        };
    }

    // TODO: drop it when converted to Kotlin
    @Nullable
    @Override
    public PsiElement elementToBeInvalidated() {
        return null;
    }
}

