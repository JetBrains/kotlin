/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.dataflow

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.types.KaType

/**
 * Represents smart cast information for an expression.
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaSmartCastInfo : KaLifetimeOwner {
    /**
     * Whether the smart cast is [stable](https://kotlinlang.org/spec/type-inference.html#smart-cast-sink-stability).
     */
    public val isStable: Boolean

    /**
     * The original type of the expression before the smart cast was applied.
     */
    @KaExperimentalApi
    public val originalType: KaType

    /**
     * The type with the smart cast applied.
     */
    public val smartCastType: KaType
}
