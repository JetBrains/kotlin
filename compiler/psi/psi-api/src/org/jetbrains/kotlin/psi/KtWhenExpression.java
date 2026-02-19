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

    @NotNull
    public List<KtWhenEntry> getEntries() {
        return findChildrenByType(KtNodeTypes.WHEN_ENTRY);
    }

    @Nullable
    public KtProperty getSubjectVariable() {
        return findChildByClass(KtProperty.class);
    }

    @Nullable
    public KtExpression getSubjectExpression() {
        return findChildByClass(KtExpression.class);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitWhenExpression(this, data);
    }

    @NotNull
    public PsiElement getWhenKeyword() {
        //noinspection ConstantConditions
        return findChildByType(KtTokens.WHEN_KEYWORD);
    }

    @Nullable
    public PsiElement getCloseBrace() {
        return findChildByType(KtTokens.RBRACE);
    }

    @Nullable
    public PsiElement getOpenBrace() {
        return findChildByType(KtTokens.LBRACE);
    }

    @Nullable
    public PsiElement getLeftParenthesis() {
        return findChildByType(KtTokens.LPAR);
    }

    @Nullable
    public PsiElement getRightParenthesis() {
        return findChildByType(KtTokens.RPAR);
    }

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
