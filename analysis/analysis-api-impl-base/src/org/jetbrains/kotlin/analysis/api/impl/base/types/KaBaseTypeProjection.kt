/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.types

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.types.Variance

class KaBaseStarTypeProjection(override val token: KaLifetimeToken) : KaStarTypeProjection

class KaBaseTypeArgumentWithVariance(
    type: KaType,
    variance: Variance,
    override val token: KaLifetimeToken,
) : KaTypeArgumentWithVariance {
    override val type: KaType by validityAsserted(type)

    override val variance: Variance by validityAsserted(variance)
}
