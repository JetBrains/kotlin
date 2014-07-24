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
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class CastExpressionFix extends JetIntentionAction<JetExpression> {
    private final JetType type;
    private final String renderedType;

    public CastExpressionFix(@NotNull JetExpression element, @NotNull JetType type) {
        super(element);
        this.type = type;
        renderedType = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(type);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("cast.expression.to.type", element.getText(), renderedType);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("cast.expression.family");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) return false;
        BindingContext context = ResolvePackage.getBindingContext((JetFile) file);
        JetType expressionType = context.get(BindingContext.EXPRESSION_TYPE, element);
        return expressionType != null && JetTypeChecker.DEFAULT.isSubtypeOf(type, expressionType);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        JetPsiFactory psiFactory = JetPsiFactory(file);
        JetBinaryExpressionWithTypeRHS castedExpression =
                (JetBinaryExpressionWithTypeRHS) psiFactory.createExpression("(" + element.getText() + ") as " + renderedType);
        if (JetPsiUtil.areParenthesesUseless((JetParenthesizedExpression) castedExpression.getLeft())) {
            castedExpression = (JetBinaryExpressionWithTypeRHS) psiFactory.createExpression(element.getText() + " as " + renderedType);
        }

        JetParenthesizedExpression castedExpressionInParentheses =
                (JetParenthesizedExpression) element.replace(psiFactory.createExpression("(" + castedExpression.getText() + ")"));

        if (JetPsiUtil.areParenthesesUseless(castedExpressionInParentheses)) {
            castedExpressionInParentheses.replace(castedExpression);
        }
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactoryForAutoCastImpossible() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                DiagnosticWithParameters2<JetExpression, JetType, String> diagnosticWithParameters =
                        Errors.AUTOCAST_IMPOSSIBLE.cast(diagnostic);
                return new CastExpressionFix(diagnosticWithParameters.getPsiElement(), diagnosticWithParameters.getA());
            }
        };
    }
}
