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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters2;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.idea.util.ShortenReferences;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public class CastExpressionFix extends JetIntentionAction<JetExpression> {
    private final JetType type;

    public CastExpressionFix(@NotNull JetExpression element, @NotNull JetType type) {
        super(element);
        this.type = type;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message(
                "cast.expression.to.type",
                element.getText(),
                IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)
        );
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("cast.expression.family");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) return false;
        JetType expressionType = ResolvePackage.analyze(element).getType(element);
        return expressionType != null && JetTypeChecker.DEFAULT.isSubtypeOf(type, expressionType);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        String renderedType = IdeDescriptorRenderers.SOURCE_CODE.renderType(type);

        JetPsiFactory psiFactory = JetPsiFactory(file);
        JetBinaryExpressionWithTypeRHS castExpression =
                (JetBinaryExpressionWithTypeRHS) psiFactory.createExpression("(" + element.getText() + ") as " + renderedType);
        if (JetPsiUtil.areParenthesesUseless((JetParenthesizedExpression) castExpression.getLeft())) {
            castExpression = (JetBinaryExpressionWithTypeRHS) psiFactory.createExpression(element.getText() + " as " + renderedType);
        }

        JetParenthesizedExpression castExpressionInParentheses =
                (JetParenthesizedExpression) element.replace(psiFactory.createExpression("(" + castExpression.getText() + ")"));

        if (JetPsiUtil.areParenthesesUseless(castExpressionInParentheses)) {
            castExpression = (JetBinaryExpressionWithTypeRHS) castExpressionInParentheses.replace(castExpression);
        }
        else {
            castExpression = (JetBinaryExpressionWithTypeRHS) castExpressionInParentheses.getExpression();
            assert castExpression != null;
        }

        JetTypeReference typeRef = castExpression.getRight();
        assert typeRef != null;
        ShortenReferences.DEFAULT.process(typeRef);
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactoryForSmartCastImpossible() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(@NotNull Diagnostic diagnostic) {
                DiagnosticWithParameters2<JetExpression, JetType, String> diagnosticWithParameters =
                        Errors.SMARTCAST_IMPOSSIBLE.cast(diagnostic);
                return new CastExpressionFix(diagnosticWithParameters.getPsiElement(), diagnosticWithParameters.getA());
            }
        };
    }
}
