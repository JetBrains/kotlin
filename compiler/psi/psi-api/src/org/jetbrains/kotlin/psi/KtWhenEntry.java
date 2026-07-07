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

    /**
     * Returns the {@code else} keyword if this is the {@code else} branch, or {@code null} otherwise.
     */
    @Nullable
    public PsiElement getElseKeyword() {
        return findChildByType(KtTokens.ELSE_KEYWORD);
    }

    /**
     * Returns the body expression of this branch (the part after {@code ->}), or {@code null} if it is absent in
     * incomplete code.
     */
    @Nullable
    public KtExpression getExpression() {
        return findChildByClass(KtExpression.class);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitWhenEntry(this, data);
    }

    /**
     * Returns the comma-separated conditions of this branch. Empty for the {@code else} branch; a single-element array
     * for a normal branch, or several elements when the branch lists multiple conditions.
     */
    @NotNull
    public KtWhenCondition[] getConditions() {
        return findChildrenByClass(KtWhenCondition.class);
    }

    /**
     * Returns the guard clause of this branch (the {@code if} guard after the conditions), or {@code null} if the
     * branch has no guard.
     */
    @Nullable
    public KtWhenEntryGuard getGuard() {
        return findChildByClass(KtWhenEntryGuard.class);
    }

    /**
     * Returns the trailing comma after the last condition, or {@code null} if there is none.
     */
    public PsiElement getTrailingComma() {
        return KtPsiUtilKt.getTrailingCommaByClosingElement(getArrow());
    }

    /**
     * Returns the {@code ->} arrow separating the conditions from the body, or {@code null} if it is absent in
     * incomplete code.
     */
    @Nullable
    public PsiElement getArrow() {
        return findChildByType(KtTokens.ARROW);
    }
}
