/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.resolution.KtResolvableCall;

import java.util.List;

/**
 * Represents an element with call syntax: a callee together with type arguments and value arguments.
 *
 * <p>This is the common base type for call-shaped nodes such as {@link KtCallExpression} (a function call),
 * {@link KtAnnotationEntry} (an annotation application), {@link KtSuperTypeCallEntry} (a superclass constructor call),
 * and {@link KtConstructorDelegationCall} (a {@code this(...)}/{@code super(...)} delegation).
 *
 * @see KtResolvableCall for resolving the call to its target
 */
public interface KtCallElement extends KtElement, KtResolvableCall {
    /**
     * Returns the callee, that is, the expression that denotes what is being called, or {@code null} if it is absent in
     * incomplete code.
     */
    @Nullable
    KtExpression getCalleeExpression();

    /**
     * Returns the parenthesized argument list, or {@code null} if the call has no parentheses (for example, a call with
     * only a trailing lambda).
     */
    @Nullable
    KtValueArgumentList getValueArgumentList();

    /**
     * Returns all value arguments of the call, including both parenthesized arguments and trailing lambda arguments.
     * Returns an empty list if there are none.
     */
    @NotNull
    List<? extends ValueArgument> getValueArguments();

    /**
     * Returns the trailing lambda arguments written outside the parentheses. Normally there is at most one; a longer
     * list only occurs in erroneous code. Returns an empty list if there are none.
     */
    @NotNull
    List<KtLambdaArgument> getLambdaArguments();

    /**
     * Returns the explicitly written type arguments, or an empty list if none are specified (in which case they are
     * inferred).
     */
    @NotNull
    List<KtTypeProjection> getTypeArguments();

    /**
     * Returns the angle-bracketed type argument list, or {@code null} if no type arguments are specified.
     */
    @Nullable
    KtTypeArgumentList getTypeArgumentList();
}
