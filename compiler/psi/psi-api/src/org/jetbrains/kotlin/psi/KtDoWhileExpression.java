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

/**
 * Represents a {@code do}-{@code while} loop that executes its body at least once before checking the condition.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    do {
 *        x--
 *    } while (x > 0)
 * // ^_____________^
 * // The entire block from 'do' to ')'
 * }</pre>
 */
public class KtDoWhileExpression extends KtWhileExpressionBase {
    public KtDoWhileExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitDoWhileExpression(this, data);
    }

    @Nullable
    @IfNotParsed
    public PsiElement getWhileKeyword() {
        //noinspection ConstantConditions
        return findChildByType(KtTokens.WHILE_KEYWORD);
    }
}
