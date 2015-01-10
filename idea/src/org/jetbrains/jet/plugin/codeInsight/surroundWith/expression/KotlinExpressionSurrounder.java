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

import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.JetCallExpression;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetQualifiedExpression;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
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
