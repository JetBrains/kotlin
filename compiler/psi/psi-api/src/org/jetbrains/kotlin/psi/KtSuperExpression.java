/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;

/**
 * Represents a {@code super} expression that refers to the superclass implementation.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * class Foo : Bar() {
 *     override fun baz() = super.baz()
 * //                       ^___^
 * }
 * }</pre>
 */
public class KtSuperExpression extends KtInstanceExpressionWithLabel implements KtStatementExpression {

    public KtSuperExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitSuperExpression(this, data);
    }

    /**
     * class A : B, C {
     *     override fun foo() {
     *         super<B>.foo()
     *         super<C>.foo()
     *     }
     * }
     */
    @Nullable
    public KtTypeReference getSuperTypeQualifier() {
        return (KtTypeReference) findChildByType(KtNodeTypes.TYPE_REFERENCE);
    }
}
