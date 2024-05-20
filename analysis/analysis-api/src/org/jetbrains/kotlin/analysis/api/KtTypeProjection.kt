/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.types.Variance

public sealed class KaTypeProjection : KaLifetimeOwner {
    public abstract val type: KaType?
}

public typealias KtTypeProjection = KaTypeProjection

public class KaStarTypeProjection(override val token: KaLifetimeToken) : KaTypeProjection() {
    override val type: KaType? get() = withValidityAssertion { null }
}

public typealias KtStarTypeProjection = KaStarTypeProjection

public class KaTypeArgumentWithVariance(
    type: KaType,
    variance: Variance,
    override val token: KaLifetimeToken,
) : KaTypeProjection() {
    override val type: KaType by validityAsserted(type)
    public val variance: Variance by validityAsserted(variance)
}

public typealias KtTypeArgumentWithVariance = KaTypeArgumentWithVariance