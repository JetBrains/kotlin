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
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters2;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

public class CastExpressionFix extends JetIntentionAction<JetExpression> {
    private final JetType type;

    public CastExpressionFix(@NotNull JetExpression element, @NotNull JetType type) {
        super(element);
        this.type = type;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("cast.expression.to.type", element.getText(), type.toString());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("cast.expression.family");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        BindingContext context = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) file).getBindingContext();
        JetType expressionType = context.get(BindingContext.EXPRESSION_TYPE, element);
        return super.isAvailable(project, editor, file) && expressionType != null && JetTypeChecker.INSTANCE.isSubtypeOf(type, expressionType);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetBinaryExpressionWithTypeRHS castedExpression =
                (JetBinaryExpressionWithTypeRHS) JetPsiFactory.createExpression(project, "(" + element.getText() + ") as " + type.toString());
        if (JetPsiUtil.areParenthesesUseless((JetParenthesizedExpression) castedExpression.getLeft())) {
            castedExpression = (JetBinaryExpressionWithTypeRHS) JetPsiFactory.createExpression(project, element.getText() + " as " + type.toString());
        }

        JetParenthesizedExpression castedExpressionInParentheses =
                (JetParenthesizedExpression) element.replace(JetPsiFactory.createExpression(project, "(" + castedExpression.getText() + ")"));

        if (JetPsiUtil.areParenthesesUseless(castedExpressionInParentheses)) {
            castedExpressionInParentheses.replace(castedExpression);
        }
    }

    @NotNull
    public static JetIntentionActionFactory createFactoryForAutoCastImpossible() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                assert diagnostic.getFactory() == Errors.AUTOCAST_IMPOSSIBLE;
                @SuppressWarnings("unchecked")
                DiagnosticWithParameters2<JetExpression, JetType, String> diagnosticWithParameters =
                        (DiagnosticWithParameters2<JetExpression, JetType, String>) diagnostic;
                return new CastExpressionFix(diagnosticWithParameters.getPsiElement(), diagnosticWithParameters.getA());
            }
        };
    }

    @NotNull
    public static JetIntentionActionFactory createFactoryForTypeMismatch() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                assert diagnostic.getFactory() == Errors.TYPE_MISMATCH;
                @SuppressWarnings("unchecked")
                DiagnosticWithParameters2<JetExpression, JetType, JetType> diagnosticWithParameters =
                        (DiagnosticWithParameters2<JetExpression, JetType, JetType>) diagnostic;
                JetExpression expression = diagnosticWithParameters.getPsiElement();

                // we don't want to cast a cast:
                if (expression instanceof JetBinaryExpressionWithTypeRHS) {
                    return null;
                }

                // 'x: Int' - TYPE_MISMATCH might be reported on 'x', and we don't want this quickfix to be available:
                JetBinaryExpressionWithTypeRHS parentExpressionWithTypeRHS =
                        PsiTreeUtil.getParentOfType(expression, JetBinaryExpressionWithTypeRHS.class, true);
                if (parentExpressionWithTypeRHS != null && parentExpressionWithTypeRHS.getLeft() == expression) {
                    return null;
                }
                return new CastExpressionFix(expression, diagnosticWithParameters.getA());
            }
        };
    }
}
