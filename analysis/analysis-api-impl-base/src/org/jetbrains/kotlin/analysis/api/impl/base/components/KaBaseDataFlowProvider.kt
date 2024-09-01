/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaImplicitReceiverSmartCast
import org.jetbrains.kotlin.analysis.api.components.KaImplicitReceiverSmartCastKind
import org.jetbrains.kotlin.analysis.api.components.KaSmartCastInfo
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import java.util.Objects

@KaImplementationDetail
class KaBaseSmartCastInfo(
    private val backingSmartCastType: KaType,
    private val backingIsStable: Boolean,
) : KaSmartCastInfo {
    override val token: KaLifetimeToken get() = backingSmartCastType.token

    override val isStable: Boolean get() = withValidityAssertion { backingIsStable }

    override val smartCastType: KaType get() = withValidityAssertion { backingSmartCastType }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaBaseSmartCastInfo &&
                other.backingSmartCastType == backingSmartCastType &&
                other.backingIsStable == backingIsStable
    }

    override fun hashCode(): Int = Objects.hash(backingSmartCastType, backingIsStable)
}

@KaImplementationDetail
class KaBaseImplicitReceiverSmartCast(
    private val backingType: KaType,
    private val backingKind: KaImplicitReceiverSmartCastKind,
) : KaImplicitReceiverSmartCast {
    override val token: KaLifetimeToken get() = backingType.token

    override val type: KaType get() = withValidityAssertion { backingType }

    override val kind: KaImplicitReceiverSmartCastKind get() = withValidityAssertion { backingKind }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaBaseImplicitReceiverSmartCast &&
                other.backingType == backingType &&
                other.backingKind == backingKind
    }

    override fun hashCode(): Int {
        return Objects.hash(backingType, backingKind)
    }
}
