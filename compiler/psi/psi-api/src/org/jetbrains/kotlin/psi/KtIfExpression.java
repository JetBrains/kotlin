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

    @Nullable @IfNotParsed
    public KtExpression getCondition() {
        return findExpressionUnder(KtNodeTypes.CONDITION);
    }

    @Nullable @IfNotParsed
    public PsiElement getLeftParenthesis() {
        return findChildByType(KtTokens.LPAR);
    }

    @Nullable @IfNotParsed
    public PsiElement getRightParenthesis() {
        return findChildByType(KtTokens.RPAR);
    }

    @Nullable
    public KtExpression getThen() {
        return findExpressionUnder(KtNodeTypes.THEN);
    }

    @Nullable
    public KtExpression getElse() {
        return findExpressionUnder(KtNodeTypes.ELSE);
    }

    @Nullable
    public PsiElement getElseKeyword() {
        return findChildByType(KtTokens.ELSE_KEYWORD);
    }

    @NotNull
    public PsiElement getIfKeyword() {
        return findChildByType(KtTokens.IF_KEYWORD);
    }
}
