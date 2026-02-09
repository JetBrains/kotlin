/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a {@code this} expression that refers to the current receiver.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * class Foo {
 *     fun bar() = this
 * //              ^__^
 * }
 * }</pre>
 */
public class KtThisExpression extends KtInstanceExpressionWithLabel {

    public KtThisExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitThisExpression(this, data);
    }
}
