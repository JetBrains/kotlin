/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.resolution.KtResolvableCall;

import java.util.Objects;

/**
 * Represents a unary expression with an operator and a single operand.
 *
 * <p>This is the base class for:</p>
 * <ul>
 *   <li>{@link KtPrefixExpression} &mdash; operator before the operand (e.g., {@code -x})</li>
 *   <li>{@link KtPostfixExpression} &mdash; operator after the operand (e.g., {@code x++})</li>
 * </ul>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val x = -5
 * //      ^^
 * }</pre>
 *
 * @see KtPrefixExpression
 * @see KtPostfixExpression
 */
public abstract class KtUnaryExpression extends KtExpressionImplStub<KotlinPlaceHolderStub<? extends KtUnaryExpression>>
        implements KtOperationExpression, KtResolvableCall {
    @KtImplementationDetail
    public KtUnaryExpression(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    protected KtUnaryExpression(@NotNull KotlinPlaceHolderStub<? extends KtUnaryExpression> stub, @NotNull IElementType nodeType) {
        super(stub, nodeType);
    }

    /** Returns the operand of this unary expression, or {@code null} if it is absent in incomplete code. */
    @Nullable @IfNotParsed
    public abstract KtExpression getBaseExpression();

    @Override
    @NotNull
    public KtOperationReferenceExpression getOperationReference() {
        return Objects.requireNonNull(getStubOrPsiChild(KtNodeTypes.OPERATION_REFERENCE, KtOperationReferenceExpression.class));
    }

    /** Returns the element type of the operator token (for example, {@code MINUS} for {@code -} or {@code PLUSPLUS} for {@code ++}). */
    public IElementType getOperationToken() {
        return getOperationReference().getReferencedNameElementType();
    }
}
