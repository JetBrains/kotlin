/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression;

public class KtDotQualifiedExpressionElementType extends KtPlaceHolderStubElementType<KtDotQualifiedExpression> {
    public KtDotQualifiedExpressionElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtDotQualifiedExpression.class);
    }
}
