/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeTypes;

/**
 * Represents a {@code finally} block in a {@code try} expression.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    try {
 *        riskyOperation()
 *    } finally {
 *        cleanup()
 *    }
 * // ^_________^
 * // The entire block from 'finally' to the closing curly brace
 * }</pre>
 */
public class KtFinallySection extends KtElementImpl implements KtStatementExpression {
    public KtFinallySection(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitFinallySection(this, data);
    }

    public KtBlockExpression getFinalExpression() {
        return (KtBlockExpression) findChildByType(KtNodeTypes.BLOCK);
    }
}
