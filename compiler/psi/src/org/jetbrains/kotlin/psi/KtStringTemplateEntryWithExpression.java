/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderWithTextStub;

import java.util.Collections;
import java.util.List;

public abstract class KtStringTemplateEntryWithExpression extends KtStringTemplateEntry {
    public KtStringTemplateEntryWithExpression(@NotNull ASTNode node) {
        super(node);
    }

    public KtStringTemplateEntryWithExpression(
            @NotNull KotlinPlaceHolderWithTextStub<? extends KtStringTemplateEntryWithExpression> stub,
            @NotNull IStubElementType elementType
    ) {
        super(stub, elementType);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitStringTemplateEntryWithExpression(this, data);
    }

    /**
     * Returns a list of expressions from this entry.
     * <p>
     * The list size is 1 for valid code, so {@link #getExpression } should be preferred
     * and this method should be used only to process potentially error code.
     *
     * @return list with expressions or empty
     * @see #getExpression
     */
    @NotNull
    public List<KtExpression> getExpressions() {
        KtExpression expression = getExpression();
        return expression != null ? Collections.singletonList(expression) : Collections.emptyList();
    }
}
