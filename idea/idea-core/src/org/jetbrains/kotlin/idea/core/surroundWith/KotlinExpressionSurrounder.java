/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.surroundWith;

import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.psi.KtCallExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtQualifiedExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.types.KotlinType;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isUnit;
import static org.jetbrains.kotlin.idea.core.surroundWith.KotlinSurrounderUtils.isUsedAsStatement;

public abstract class KotlinExpressionSurrounder implements Surrounder {

    @Override
    public boolean isApplicable(@NotNull PsiElement[] elements) {
        if (elements.length != 1 || !(elements[0] instanceof KtExpression)) {
            return false;
        }

        KtExpression expression = (KtExpression) elements[0];
        if (expression instanceof KtCallExpression && expression.getParent() instanceof KtQualifiedExpression) {
            return false;
        }

        return isApplicable(expression);
    }

    protected boolean isApplicable(@NotNull KtExpression expression) {
        BindingContext context = ResolutionUtils.analyze(expression, BodyResolveMode.PARTIAL);
        KotlinType type = context.getType(expression);
        if (type == null || (isUnit(type) && isApplicableToStatements())) {
            return false;
        }

        if (!isApplicableToStatements() && isUsedAsStatement(expression)) {
            return false;
        }

        return true;
    }

    protected boolean isApplicableToStatements() {
        return true;
    }

    @Nullable
    @Override
    public TextRange surroundElements(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement[] elements) {
        assert elements.length == 1 : "KotlinExpressionSurrounder should be applicable only for 1 expression: " + elements.length;
        return surroundExpression(project, editor, (KtExpression) elements[0]);
    }

    @Nullable
    protected abstract TextRange surroundExpression(@NotNull Project project, @NotNull Editor editor, @NotNull KtExpression expression);
}
