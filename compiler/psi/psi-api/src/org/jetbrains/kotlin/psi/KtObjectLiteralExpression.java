/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

import java.util.Objects;

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
public class KtObjectLiteralExpression extends KtExpressionImplStub<KotlinPlaceHolderStub<KtObjectLiteralExpression>> {
    public KtObjectLiteralExpression(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    public KtObjectLiteralExpression(@NotNull KotlinPlaceHolderStub<KtObjectLiteralExpression> stub) {
        super(stub, KtStubBasedElementTypes.OBJECT_LITERAL);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitObjectLiteralExpression(this, data);
    }

    /** Returns the anonymous object declaration wrapped by this expression. */
    @NotNull
    @SuppressWarnings("deprecation") // KT-78356
    public KtObjectDeclaration getObjectDeclaration() {
        return Objects.requireNonNull(getStubOrPsiChild(KtStubBasedElementTypes.OBJECT_DECLARATION));
    }
}
