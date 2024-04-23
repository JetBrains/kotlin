/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.types.Variance

public sealed class KtTypeProjection : KtLifetimeOwner {
    public abstract val type: KtType?
}

public class KtStarTypeProjection(override val token: KtLifetimeToken) : KtTypeProjection() {
    override val type: KtType? get() = withValidityAssertion { null }
}

public class KtTypeArgumentWithVariance(
    type: KtType,
    variance: Variance,
    override val token: KtLifetimeToken,
) : KtTypeProjection() {
    override val type: KtType by validityAsserted(type)
    public val variance: Variance by validityAsserted(variance)
}
