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
import org.jetbrains.kotlin.resolution.KtResolvable;

/**
 * Represents a {@code return} expression that returns a value from a function or lambda.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * fun main() {
 *     return
 * //  ^____^
 * }
 * }</pre>
 */
public class KtReturnExpression extends KtExpressionWithLabel implements KtStatementExpression, KtResolvable {
    public KtReturnExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitReturnExpression(this, data);
    }

    /**
     * Returns the value being returned, or {@code null} for a bare {@code return}.
     */
    @Nullable
    public KtExpression getReturnedExpression() {
        return findChildByClass(KtExpression.class);
    }

    /**
     * Returns the {@code return} keyword.
     */
    @NotNull
    public PsiElement getReturnKeyword() {
        //noinspection ConstantConditions
        return findChildByType(KtTokens.RETURN_KEYWORD);
    }

    /**
     * Returns the label qualifier of a labeled return ({@code return@label}), or {@code null} if the return is
     * unlabeled.
     */
    @Nullable
    public PsiElement getLabeledExpression() {
        return findChildByType(KtNodeTypes.LABEL_QUALIFIER);
    }
}
