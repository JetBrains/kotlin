/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a {@code while} loop that executes its body while the condition is true.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    while (x > 0) {
 *        x--
 *    }
 * // ^_____________^
 * // The entire block from 'while' to the closing curly brace
 * }</pre>
 */
public class KtWhileExpression extends KtWhileExpressionBase {
    public KtWhileExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitWhileExpression(this, data);
    }
}
