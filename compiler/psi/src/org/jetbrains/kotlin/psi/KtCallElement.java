/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.resolution.KtResolvableCall;

import java.util.List;

public interface KtCallElement extends KtElement, KtResolvableCall {
    @Nullable
    KtExpression getCalleeExpression();

    @Nullable
    KtValueArgumentList getValueArgumentList();

    @NotNull
    List<? extends ValueArgument> getValueArguments();

    @NotNull
    List<KtLambdaArgument> getLambdaArguments();

    @NotNull
    List<KtTypeProjection> getTypeArguments();

    @Nullable
    KtTypeArgumentList getTypeArgumentList();
}
