/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets;

/**
 * Represents an expression enclosed in parentheses.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val x = (1 + 2) * 3
 * //      ^_____^
 * }</pre>
 */
public class KtParenthesizedExpression extends KtExpressionImplStub<KotlinPlaceHolderStub<KtParenthesizedExpression>> {
    public KtParenthesizedExpression(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    public KtParenthesizedExpression(@NotNull KotlinPlaceHolderStub<KtParenthesizedExpression> stub) {
        super(stub, KtStubBasedElementTypes.PARENTHESIZED);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitParenthesizedExpression(this, data);
    }

    @Nullable @IfNotParsed
    public KtExpression getExpression() {
        KotlinPlaceHolderStub<KtParenthesizedExpression> stub = getStub();
        if (stub != null) {
            KtExpression[] expressions = stub.getChildrenByType(KtTokenSets.CONSTANT_EXPRESSIONS, KtExpression.EMPTY_ARRAY);
            if (expressions.length != 0) {
                return expressions[0];
            }
        }

        return findChildByClass(KtExpression.class);
    }
}
