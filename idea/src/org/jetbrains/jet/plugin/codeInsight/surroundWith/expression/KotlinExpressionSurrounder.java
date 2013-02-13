package org.jetbrains.jet.plugin.codeInsight.surroundWith.expression;

import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetExpression;

public abstract class KotlinExpressionSurrounder implements Surrounder {

    @Override
    public boolean isApplicable(@NotNull PsiElement[] elements) {
        if (elements.length != 1 || !(elements[0] instanceof JetExpression)) {
            return false;
        }
        return isApplicable((JetExpression) elements[0]);
    }

    protected abstract boolean isApplicable(@NotNull JetExpression expression);

    @Nullable
    @Override
    public TextRange surroundElements(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement[] elements) {
        assert elements.length == 1 : "KotlinExpressionSurrounder should be applicable only for 1 expression: " + elements.length;
        return surroundExpression(project, editor, (JetExpression) elements[0]);
    }

    @Nullable
    protected abstract TextRange surroundExpression(@NotNull Project project, @NotNull Editor editor, @NotNull JetExpression expression);
}
