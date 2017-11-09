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

package org.jetbrains.kotlin.idea.codeInsight.surroundWith;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;

public class KotlinSurrounderUtils {
    public static String SURROUND_WITH = KotlinBundle.message("surround.with");
    public static String SURROUND_WITH_ERROR = KotlinBundle.message("surround.with.cannot.perform.action");

    private KotlinSurrounderUtils() {
    }

    public static void addStatementsInBlock(
            @NotNull KtBlockExpression block,
            @NotNull PsiElement[] statements
    ) {
        PsiElement lBrace = block.getFirstChild();
        block.addRangeAfter(statements[0], statements[statements.length - 1], lBrace);
    }

    public static void showErrorHint(@NotNull Project project, @NotNull Editor editor, @NotNull String message) {
        CodeInsightUtils.showErrorHint(project, editor, message, SURROUND_WITH, null);
    }

    public static boolean isUsedAsStatement(@NotNull KtExpression expression) {
        BindingContext context = ResolutionUtils.analyze(expression, BodyResolveMode.PARTIAL);
        return BindingContextUtilsKt.isUsedAsStatement(expression, context);
    }

    public static boolean isUsedAsExpression(@NotNull KtExpression expression) {
        BindingContext context = ResolutionUtils.analyze(expression, BodyResolveMode.PARTIAL);
        return BindingContextUtilsKt.isUsedAsExpression(expression, context);
    }
}
