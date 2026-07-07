/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderWithTextStub;

import java.util.List;

/**
 * Represents a single entry of a {@link KtStringTemplateExpression}, that is, one segment of a string literal.
 *
 * <p>A string is split into consecutive entries: plain literal text ({@link KtLiteralStringTemplateEntry}), escape
 * sequences ({@link KtEscapeStringTemplateEntry}), and interpolated expressions ({@link KtStringTemplateEntryWithExpression}).
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *   "Hello, $name!"
 * // ^^^^^^^       literal entry "Hello, "
 * //        ^^^^^  simple-name entry "$name"
 * //             ^ literal entry "!"
 * }</pre>
 */
public abstract class KtStringTemplateEntry extends KtElementImplStub<KotlinPlaceHolderWithTextStub<? extends KtStringTemplateEntry>> {
    /**
     * A shared empty array, useful as a zero-length return value.
     */
    public static final KtStringTemplateEntry[] EMPTY_ARRAY = new KtStringTemplateEntry[0];

    public KtStringTemplateEntry(@NotNull ASTNode node) {
        super(node);
    }

    public KtStringTemplateEntry(
            @NotNull KotlinPlaceHolderWithTextStub<? extends KtStringTemplateEntry> stub,
            @NotNull IStubElementType elementType
    ) {
        super(stub, elementType);
    }

    /**
     * Retrieves the child expression if available.
     *
     * @return the child {@link KtExpression} or null if no expression is found
     */
    @Nullable
    public KtExpression getExpression() {
        // not green stub is used on purpose as it should be more performant
        // to search directly via ast if possible
        KotlinPlaceHolderStub<?> stub = getStub();
        if (stub != null) {
            List<StubElement<?>> childrenStubs = stub.getChildrenStubs();
            for (StubElement<?> element : childrenStubs) {
                PsiElement psiElement = element.getPsi();
                if (psiElement instanceof KtExpression) {
                    return (KtExpression) psiElement;
                }
            }
        }

        return findChildByClass(KtExpression.class);
    }

    @Override
    public String getText() {
        KotlinPlaceHolderWithTextStub<?> stub = getGreenStub();
        if (stub != null) {
            return stub.getText();
        }

        return super.getText();
    }
}
