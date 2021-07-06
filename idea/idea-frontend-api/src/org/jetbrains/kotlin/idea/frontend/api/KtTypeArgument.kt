/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.types.Variance

public sealed class KtTypeArgument : ValidityTokenOwner

public class KtStarProjectionTypeArgument(override val token: ValidityToken) : KtTypeArgument()

public class KtTypeArgumentWithVariance(
    public val type: KtType,
    public val variance: Variance,
    override val token: ValidityToken,
) : KtTypeArgument()

