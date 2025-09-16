/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.types

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.types.Variance

@KaImplementationDetail
class KaBaseStarTypeProjection(override val token: KaLifetimeToken) : KaStarTypeProjection

@KaImplementationDetail
class KaBaseTypeArgumentWithVariance(
    type: KaType,
    variance: Variance,
    override val token: KaLifetimeToken,
) : KaTypeArgumentWithVariance {
    private val backingType: KaType = type
    private val backingVariance: Variance = variance

    override val type: KaType get() = withValidityAssertion { backingType }

    override val variance: Variance get() = withValidityAssertion { backingVariance }
}
