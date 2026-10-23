/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.dataflow

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import java.util.*

internal class KaFirSmartCastInfo(
    private val backingOriginalType: KaType,
    private val backingSmartCastType: KaType,
    private val backingIsStable: Boolean,
) : org.jetbrains.kotlin.analysis.api.components.KaSmartCastInfo {
    override val token: KaLifetimeToken get() = backingSmartCastType.token

    override val isStable: Boolean get() = withValidityAssertion { backingIsStable }

    override val originalType: KaType get() = withValidityAssertion { backingOriginalType }

    override val smartCastType: KaType get() = withValidityAssertion { backingSmartCastType }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaFirSmartCastInfo &&
                other.backingSmartCastType == backingSmartCastType &&
                other.backingIsStable == backingIsStable
    }

    override fun hashCode(): Int = Objects.hash(backingSmartCastType, backingIsStable)
}
