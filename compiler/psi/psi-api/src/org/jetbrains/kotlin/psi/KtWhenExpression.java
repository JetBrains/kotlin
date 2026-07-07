/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;

import java.util.List;

/**
 * Represents a {@code when} expression with an optional subject and multiple branches.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    val result = when (x) {
 *        1 -> "one"
 *        else -> "other"
 *    }
 * // ^_____________________^
 * // The entire block from 'when' to the closing curly brace
 * }</pre>
 */
public class KtWhenExpression extends KtExpressionImpl {
    public KtWhenExpression(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Returns the branches of this {@code when}, in source order; empty if there are none.
     */
    @NotNull
    public List<KtWhenEntry> getEntries() {
        return findChildrenByType(KtNodeTypes.WHEN_ENTRY);
    }

    /**
     * Returns the subject variable declared in the {@code when} header (as in {@code when (val x = f())}), or
     * {@code null} if the subject is a plain expression or absent.
     */
    @Nullable
    public KtProperty getSubjectVariable() {
        return findChildByClass(KtProperty.class);
    }

    /**
     * Returns the subject expression of this {@code when} (the value being matched), or {@code null} for a subjectless
     * {@code when}.
     */
    @Nullable
    public KtExpression getSubjectExpression() {
        return findChildByClass(KtExpression.class);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitWhenExpression(this, data);
    }

    /**
     * Returns the {@code when} keyword.
     */
    @NotNull
    public PsiElement getWhenKeyword() {
        //noinspection ConstantConditions
        return findChildByType(KtTokens.WHEN_KEYWORD);
    }

    /**
     * Returns the closing brace of the {@code when} block, or {@code null} if it is absent in incomplete code.
     */
    @Nullable
    public PsiElement getCloseBrace() {
        return findChildByType(KtTokens.RBRACE);
    }

    /**
     * Returns the opening brace of the {@code when} block, or {@code null} if it is absent in incomplete code.
     */
    @Nullable
    public PsiElement getOpenBrace() {
        return findChildByType(KtTokens.LBRACE);
    }

    /**
     * Returns the opening parenthesis around the subject, or {@code null} for a subjectless {@code when}.
     */
    @Nullable
    public PsiElement getLeftParenthesis() {
        return findChildByType(KtTokens.LPAR);
    }

    /**
     * Returns the closing parenthesis around the subject, or {@code null} for a subjectless {@code when}.
     */
    @Nullable
    public PsiElement getRightParenthesis() {
        return findChildByType(KtTokens.RPAR);
    }

    /**
     * Returns the expression of the {@code else} branch, or {@code null} if this {@code when} has no {@code else}
     * branch.
     */
    @Nullable
    public KtExpression getElseExpression() {
        for (KtWhenEntry entry : getEntries()) {
            if (entry.isElse()) {
                return entry.getExpression();
            }
        }
        return null;
    }
}
