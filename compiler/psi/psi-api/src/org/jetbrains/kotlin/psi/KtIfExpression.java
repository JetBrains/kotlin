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

/**
 * Represents an {@code if} expression with condition, then branch, and optional {@code else} branch.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val max = if (a > b) a else b
 * //        ^_________________^
 * }</pre>
 */
public class KtIfExpression extends KtExpressionImpl {
    public KtIfExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitIfExpression(this, data);
    }

    /**
     * Returns the condition expression, or {@code null} if it is absent in incomplete code.
     */
    @Nullable @IfNotParsed
    public KtExpression getCondition() {
        return findExpressionUnder(KtNodeTypes.CONDITION);
    }

    /**
     * Returns the opening parenthesis around the condition, or {@code null} if it is absent in incomplete code.
     */
    @Nullable @IfNotParsed
    public PsiElement getLeftParenthesis() {
        return findChildByType(KtTokens.LPAR);
    }

    /**
     * Returns the closing parenthesis around the condition, or {@code null} if it is absent in incomplete code.
     */
    @Nullable @IfNotParsed
    public PsiElement getRightParenthesis() {
        return findChildByType(KtTokens.RPAR);
    }

    /**
     * Returns the {@code then} branch (executed when the condition is true), or {@code null} if it is absent in
     * incomplete code.
     */
    @Nullable
    public KtExpression getThen() {
        return findExpressionUnder(KtNodeTypes.THEN);
    }

    /**
     * Returns the {@code else} branch, or {@code null} if there is no {@code else}.
     */
    @Nullable
    public KtExpression getElse() {
        return findExpressionUnder(KtNodeTypes.ELSE);
    }

    /**
     * Returns the {@code else} keyword, or {@code null} if there is no {@code else} branch.
     */
    @Nullable
    public PsiElement getElseKeyword() {
        return findChildByType(KtTokens.ELSE_KEYWORD);
    }

    /**
     * Returns the {@code if} keyword.
     */
    @NotNull
    public PsiElement getIfKeyword() {
        return findChildByType(KtTokens.IF_KEYWORD);
    }
}
