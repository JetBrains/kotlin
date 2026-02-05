/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a {@code continue} expression that skips to the next iteration of the enclosing loop.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * for (i in 1..10) {
 *     if (i == 5) continue
 * //              ^______^
 * }
 * }</pre>
 */
public class KtContinueExpression extends KtExpressionWithLabel implements KtStatementExpression {
    public KtContinueExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitContinueExpression(this, data);
    }
}
