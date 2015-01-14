/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.codeInsight.surroundWith;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.JetBlockExpression;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.codeInsight.CodeInsightUtils;

public class KotlinSurrounderUtils {
    public static String SURROUND_WITH = JetBundle.message("surround.with");
    public static String SURROUND_WITH_ERROR = JetBundle.message("surround.with.cannot.perform.action");

    private KotlinSurrounderUtils() {
    }

    public static void addStatementsInBlock(
            @NotNull JetBlockExpression block,
            @NotNull PsiElement[] statements
    ) {
        PsiElement lBrace = block.getFirstChild();
        block.addRangeAfter(statements[0], statements[statements.length - 1], lBrace);
    }

    @Nullable
    public static JetType getExpressionType(JetExpression expression) {
        BindingContext expressionBindingContext = ResolvePackage.analyze(expression);
        return expressionBindingContext.get(BindingContext.EXPRESSION_TYPE, expression);
    }

    public static void showErrorHint(@NotNull Project project, @NotNull Editor editor, @NotNull String message) {
        CodeInsightUtils.showErrorHint(project, editor, message, SURROUND_WITH, null);
    }
}
