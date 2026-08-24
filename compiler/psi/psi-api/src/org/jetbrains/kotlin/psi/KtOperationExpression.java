/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an expression built around an operation sign, such as a unary, binary, or "is"/"as" expression.
 *
 * <p>The operation sign itself (for example, {@code +}, {@code !}, or {@code in}) is exposed as a {@link KtOperationReferenceExpression},
 * which can be resolved to the operator or conversion function it stands for.
 *
 * @see KtBinaryExpression
 * @see KtUnaryExpression
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public interface KtOperationExpression extends KtExpression {
    /** Returns the operation sign of this expression as a reference that can be resolved to the corresponding function. */
    @NotNull
    KtSimpleNameExpression getOperationReference();
}
