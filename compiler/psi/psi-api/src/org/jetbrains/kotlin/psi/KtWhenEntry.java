/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;

/**
 * Represents a single branch in a {@code when} expression.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * when (x) {
 *     1 -> "one"
 * //  ^_________^
 *     else -> "other"
 * }
 * }</pre>
 */
public class KtWhenEntry extends KtElementImpl {
    public KtWhenEntry(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * @return {@code true} if this is an {@code else} condition with no {@link #getGuard() guard}, {@code false} otherwise.
     */
    public boolean isElse() {
        return getElseKeyword() != null && getGuard() == null;
    }

    @Nullable
    public PsiElement getElseKeyword() {
        return findChildByType(KtTokens.ELSE_KEYWORD);
    }

    @Nullable
    public KtExpression getExpression() {
        return findChildByClass(KtExpression.class);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitWhenEntry(this, data);
    }

    @NotNull
    public KtWhenCondition[] getConditions() {
        return findChildrenByClass(KtWhenCondition.class);
    }

    @Nullable
    public KtWhenEntryGuard getGuard() {
        return findChildByClass(KtWhenEntryGuard.class);
    }

    public PsiElement getTrailingComma() {
        return KtPsiUtilKt.getTrailingCommaByClosingElement(getArrow());
    }

    @Nullable
    public PsiElement getArrow() {
        return findChildByType(KtTokens.ARROW);
    }
}
