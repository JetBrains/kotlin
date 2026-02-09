/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeTypes;

/**
 * Represents an {@code object} literal expression that creates an anonymous object.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    val obj = object : Runnable {
 *        override fun run() {}
 *    }
 * // ^___________________________^
 * // The entire block from 'object :' to the closing curly brace
 * }</pre>
 */
public class KtObjectLiteralExpression extends KtExpressionImpl {
    public KtObjectLiteralExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitObjectLiteralExpression(this, data);
    }

    @NotNull
    public KtObjectDeclaration getObjectDeclaration() {
        return (KtObjectDeclaration) findChildByType(KtNodeTypes.OBJECT_DECLARATION);
    }
}
