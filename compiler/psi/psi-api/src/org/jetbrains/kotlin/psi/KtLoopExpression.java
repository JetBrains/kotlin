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
 * Represents a loop expression: a {@code for}, {@code while}, or {@code do}-{@code while} loop.
 *
 * <p>This is the common base for the concrete node types {@link KtForExpression}, {@link KtWhileExpression}, and
 * {@link KtDoWhileExpression}. It gives access to the loop body shared by all loop kinds.
 */
public abstract class KtLoopExpression extends KtExpressionImpl implements KtStatementExpression {
    @KtImplementationDetail
    public KtLoopExpression(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Returns the loop body, or {@code null} if it is absent in incomplete code. The body may be a {@link KtBlockExpression} or a
     * single statement.
     */
    @Nullable
    public KtExpression getBody() {
        return findExpressionUnder(KtNodeTypes.BODY);
    }

    /** Returns the opening parenthesis of the loop header, or {@code null} if it is absent in incomplete code. */
    @Nullable
    @IfNotParsed
    public PsiElement getLeftParenthesis() {
        return findChildByType(KtTokens.LPAR);
    }

    /** Returns the closing parenthesis of the loop header, or {@code null} if it is absent in incomplete code. */
    @Nullable @IfNotParsed
    public PsiElement getRightParenthesis() {
        return findChildByType(KtTokens.RPAR);
    }
}
