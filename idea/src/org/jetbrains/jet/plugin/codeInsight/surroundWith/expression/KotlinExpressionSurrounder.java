package org.jetbrains.jet.plugin.codeInsight.surroundWith.expression;

import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.codeInsight.surroundWith.KotlinSurrounderUtils;

public abstract class KotlinExpressionSurrounder implements Surrounder {

    @Override
    public boolean isApplicable(@NotNull PsiElement[] elements) {
        if (elements.length != 1 || !(elements[0] instanceof JetExpression)) {
            return false;
        }

        JetExpression expression = (JetExpression) elements[0];
        if (expression instanceof JetCallExpression && expression.getParent() instanceof JetQualifiedExpression) {
            return false;
        }
        JetType type = KotlinSurrounderUtils.getExpressionType(expression);
        if (type == null || type.equals(KotlinBuiltIns.getInstance().getUnitType())) {
            return false;
        }
        return isApplicable(expression);
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
