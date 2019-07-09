/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.surroundWith;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.core.util.CodeInsightUtils;
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
        BindingContext context = ResolutionUtils.analyze(expression, BodyResolveMode.PARTIAL_WITH_CFA);
        return BindingContextUtilsKt.isUsedAsStatement(expression, context);
    }

    public static boolean isUsedAsExpression(@NotNull KtExpression expression) {
        BindingContext context = ResolutionUtils.analyze(expression, BodyResolveMode.PARTIAL_WITH_CFA);
        return BindingContextUtilsKt.isUsedAsExpression(expression, context);
    }
}
