/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.codeInsight.surroundWith.expression;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetParenthesizedExpression;
import org.jetbrains.jet.lang.psi.JetPrefixExpression;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.codeInsight.surroundWith.KotlinSurrounderUtils;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class KotlinNotSurrounder extends KotlinExpressionSurrounder {
    @Override
    public String getTemplateDescription() {
        return CodeInsightBundle.message("surround.with.not.template");
    }

    @Override
    public boolean isApplicable(@NotNull JetExpression expression) {
        JetType type = KotlinSurrounderUtils.getExpressionType(expression);
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
