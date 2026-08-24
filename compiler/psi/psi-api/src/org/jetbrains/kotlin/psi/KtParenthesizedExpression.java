/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

/**
 * Represents an expression enclosed in parentheses.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val x = (1 + 2) * 3
 * //      ^_____^
 * }</pre>
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public class KtParenthesizedExpression extends KtExpressionImplStub<KotlinPlaceHolderStub<KtParenthesizedExpression>> {
    @KtImplementationDetail
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

    /** Returns the expression inside the parentheses, or {@code null} if it is absent in incomplete code. */
    @Nullable @IfNotParsed
    public KtExpression getExpression() {
        KtExpression fromStub = getExpressionFromStub();
        if (fromStub != null) {
            return fromStub;
        }

        return findChildByClass(KtExpression.class);
    }
}
