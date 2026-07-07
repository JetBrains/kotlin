/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a declaration that can have a body, such as a function, a property accessor, or an anonymous initializer.
 *
 * <p>A body comes in two forms: a <em>block body</em> enclosed in braces, or an <em>expression body</em> introduced by
 * {@code =}. Both are returned by {@link #getBodyExpression()}; use {@link #hasBlockBody()} to distinguish them.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * fun square(x: Int) = x * x     // expression body
 * fun greet() { println("hi") }  // block body
 * }</pre>
 */
public interface KtDeclarationWithBody extends KtDeclaration {
    /**
     * Returns the body of this declaration, or {@code null} if it is abstract or otherwise has no body. The body is
     * either a {@link KtBlockExpression} (block body) or an arbitrary {@link KtExpression} (expression body).
     */
    @Nullable
    KtExpression getBodyExpression();

    /**
     * Returns the {@code =} token that introduces an expression body, or {@code null} if this declaration has no
     * expression body.
     */
    @Nullable
    PsiElement getEqualsToken();

    @Override
    @Nullable
    String getName();

    /**
     * Returns the contract effects declared in this declaration's body, or {@code null} if it declares no contract.
     */
    @Nullable
    default KtContractEffectList getContractDescription() {
        return null;
    }

    /**
     * Returns {@code true} if this declaration has a contract effect list (see {@link #getContractDescription()}).
     */
    default boolean hasContractEffectList() {
        return getContractDescription() != null;
    }

    /**
     * Whether the declaration may have a contract.
     * <p>
     * <b>false</b> means that the declaration definitely has no contract,
     * but <b>true</b> doesn't guarantee that the declaration has a contract.
     */
    default boolean mayHaveContract() {
        return false;
    }

    /**
     * Returns {@code true} if this declaration has a block body (enclosed in braces) rather than an expression body.
     */
    boolean hasBlockBody();

    /**
     * Returns {@code true} if this declaration has a body of either form (block or expression).
     */
    boolean hasBody();

    /**
     * Returns {@code true} if this declaration has an explicitly written return type.
     */
    boolean hasDeclaredReturnType();

    /**
     * Returns the value parameters of this declaration, or an empty list if there are none.
     */
    @NotNull
    List<KtParameter> getValueParameters();

    /**
     * Returns the body as a {@link KtBlockExpression} if this declaration has a block body, or {@code null} otherwise
     * (an expression body or no body at all).
     */
    @Nullable
    default KtBlockExpression getBodyBlockExpression() {
        KtExpression bodyExpression = getBodyExpression();
        if (bodyExpression instanceof KtBlockExpression) {
            return (KtBlockExpression) bodyExpression;
        }

        return null;
    }
}

