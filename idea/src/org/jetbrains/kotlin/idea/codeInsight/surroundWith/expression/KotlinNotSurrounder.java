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
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtParenthesizedExpression;
import org.jetbrains.kotlin.psi.KtPrefixExpression;
import org.jetbrains.kotlin.psi.KtPsiFactoryKt;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.types.KotlinType;

public class KotlinNotSurrounder extends KotlinExpressionSurrounder {
    @Override
    public String getTemplateDescription() {
        return CodeInsightBundle.message("surround.with.not.template");
    }

    @Override
    public boolean isApplicable(@NotNull KtExpression expression) {
        if (!super.isApplicable(expression)) return false;
        KotlinType type = ResolutionUtils.analyze(expression, BodyResolveMode.PARTIAL).getType(expression);
        return type != null && KotlinBuiltIns.isBoolean(type);
    }

    @Nullable
    @Override
    public TextRange surroundExpression(@NotNull Project project, @NotNull Editor editor, @NotNull KtExpression expression) {
        KtPrefixExpression prefixExpr = (KtPrefixExpression) KtPsiFactoryKt.KtPsiFactory(expression).createExpression("!(a)");
        KtParenthesizedExpression parenthesizedExpression = (KtParenthesizedExpression) prefixExpr.getBaseExpression();
        assert parenthesizedExpression != null : "JetParenthesizedExpression should exists for " + prefixExpr.getText() + " expression";
        KtExpression expressionWithoutParentheses = parenthesizedExpression.getExpression();
        assert expressionWithoutParentheses != null : "JetExpression should exists for " + parenthesizedExpression.getText() + " expression";
        expressionWithoutParentheses.replace(expression);

        expression = (KtExpression) expression.replace(prefixExpr);

        CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(expression);

        int offset = expression.getTextRange().getEndOffset();
        return new TextRange(offset, offset);
    }
}
