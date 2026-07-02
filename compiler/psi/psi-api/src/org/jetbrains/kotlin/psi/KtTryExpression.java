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
 * Represents a {@code try} expression with {@code catch} clauses and optional {@code finally} block.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    try {
 *        riskyOperation()
 *    } catch (e: Exception) {
 *        handleError(e)
 *    }
 * // ^______________________^
 * // The entire block from 'try'
 * }</pre>
 */
public class KtTryExpression extends KtExpressionImpl {
    public KtTryExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitTryExpression(this, data);
    }

    /**
     * Returns the protected {@code try} block.
     */
    @NotNull
    public KtBlockExpression getTryBlock() {
        return (KtBlockExpression) findChildByType(KtNodeTypes.BLOCK);
    }

    /**
     * Returns the {@code catch} clauses, in source order; empty if there are none.
     */
    @NotNull
    public List<KtCatchClause> getCatchClauses() {
        return findChildrenByType(KtNodeTypes.CATCH);
    }

    /**
     * Returns the {@code finally} block, or {@code null} if there is none.
     */
    @Nullable
    public KtFinallySection getFinallyBlock() {
        return (KtFinallySection) findChildByType(KtNodeTypes.FINALLY);
    }

    /**
     * Returns the {@code try} keyword, or {@code null} if it is absent in incomplete code.
     */
    @Nullable
    public PsiElement getTryKeyword() {
        return findChildByType(KtTokens.TRY_KEYWORD);
    }
}
