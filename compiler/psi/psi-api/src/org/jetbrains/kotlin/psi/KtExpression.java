/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.util.ArrayFactory;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a Kotlin expression: a piece of code that can be evaluated to a value, such as a literal, an operator application, a function
 * call, or an {@code if}/{@code when} used as a value.
 *
 * <p>This is the common base type for all expression nodes in the Kotlin PSI. In the Kotlin grammar statements are a subset of expressions,
 * so control-flow constructs (loops, {@code return}, {@code throw}) and even local declarations ({@link KtDeclaration}) are modeled as
 * {@link KtExpression}s as well.
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public interface KtExpression extends KtElement {
    /** A shared empty array, useful as a zero-length return value. */
    KtExpression[] EMPTY_ARRAY = new KtExpression[0];

    /** A factory for creating arrays of {@link KtExpression}, used by the PSI child-access machinery. */
    ArrayFactory<KtExpression> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new KtExpression[count];

    @Override
    <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data);
}
