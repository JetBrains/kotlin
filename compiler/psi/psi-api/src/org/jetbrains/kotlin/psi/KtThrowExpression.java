/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a {@code throw} expression that throws an exception.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    throw IllegalArgumentException("Invalid value")
 * // ^_____________________________________________^
 * }</pre>
 */
public class KtThrowExpression extends KtExpressionImpl implements KtStatementExpression {
    public KtThrowExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitThrowExpression(this, data);
    }

    @Nullable @IfNotParsed
    public KtExpression getThrownExpression() {
        return findChildByClass(KtExpression.class);
    }
}
