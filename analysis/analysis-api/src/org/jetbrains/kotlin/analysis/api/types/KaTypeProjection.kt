/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.types.Variance

public sealed interface KaTypeProjection : KaLifetimeOwner {
    public val type: KaType?
}

public interface KaStarTypeProjection : KaTypeProjection {
    override val type: KaType? get() = withValidityAssertion { null }
}

public interface KaTypeArgumentWithVariance : KaTypeProjection {
    override val type: KaType

    public val variance: Variance
}
