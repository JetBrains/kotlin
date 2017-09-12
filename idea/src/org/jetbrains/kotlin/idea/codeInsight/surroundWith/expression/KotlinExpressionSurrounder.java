/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import static org.jetbrains.kotlin.idea.codeInsight.surroundWith.KotlinSurrounderUtils.isUsedAsStatement;

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
