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

package org.jetbrains.kotlin.idea.codeInsight.surroundWith.expression;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetParenthesizedExpression;
import org.jetbrains.kotlin.psi.JetPrefixExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.types.JetType;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public class KotlinNotSurrounder extends KotlinExpressionSurrounder {
    @Override
    public String getTemplateDescription() {
        return CodeInsightBundle.message("surround.with.not.template");
    }

    @Override
    public boolean isApplicable(@NotNull JetExpression expression) {
        JetType type = ResolvePackage.analyze(expression, BodyResolveMode.PARTIAL).get(BindingContext.EXPRESSION_TYPE, expression);
        return KotlinBuiltIns.getInstance().getBooleanType().equals(type);
    }

    @Nullable
    @Override
    public TextRange surroundExpression(@NotNull Project project, @NotNull Editor editor, @NotNull JetExpression expression) {
        JetPrefixExpression prefixExpr = (JetPrefixExpression) JetPsiFactory(expression).createExpression("!(a)");
        JetParenthesizedExpression parenthesizedExpression = (JetParenthesizedExpression) prefixExpr.getBaseExpression();
        assert parenthesizedExpression != null : "JetParenthesizedExpression should exists for " + prefixExpr.getText() + " expression";
        JetExpression expressionWithoutParentheses = parenthesizedExpression.getExpression();
        assert expressionWithoutParentheses != null : "JetExpression should exists for " + parenthesizedExpression.getText() + " expression";
        expressionWithoutParentheses.replace(expression);

        expression = (JetExpression) expression.replace(prefixExpr);

        CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(expression);

        int offset = expression.getTextRange().getEndOffset();
        return new TextRange(offset, offset);
    }
}
