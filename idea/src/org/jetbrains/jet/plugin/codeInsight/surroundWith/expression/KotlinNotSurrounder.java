package org.jetbrains.jet.plugin.codeInsight.surroundWith.expression;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;

public class KotlinNotSurrounder extends KotlinExpressionSurrounder {
    @Override
    public String getTemplateDescription() {
        return CodeInsightBundle.message("surround.with.not.template");
    }

    @Override
    public boolean isApplicable(@NotNull JetExpression expression) {
        ResolveSession resolveSession = WholeProjectAnalyzerFacade.getLazyResolveSessionForFile((JetFile) expression.getContainingFile());
        BindingContext expressionBindingContext = ResolveSessionUtils.resolveToExpression(resolveSession, expression);
        JetType type = expressionBindingContext.get(BindingContext.EXPRESSION_TYPE, expression);
        return KotlinBuiltIns.getInstance().getBooleanType().equals(type);
    }

    @Nullable
    @Override
    public TextRange surroundExpression(@NotNull Project project, @NotNull Editor editor, @NotNull JetExpression expression) {
        JetPrefixExpression prefixExpr = (JetPrefixExpression)JetPsiFactory.createExpression(project, "!(a)");
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
