/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.dataflow

import org.jetbrains.kotlin.analysis.api.dataflow.KaImplicitReceiverSmartCast
import org.jetbrains.kotlin.analysis.api.dataflow.KaImplicitReceiverSmartCastKind
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import java.util.*

internal class KaFirImplicitReceiverSmartCast(
    private val backingType: KaType,
    private val backingKind: KaImplicitReceiverSmartCastKind,
) : KaImplicitReceiverSmartCast {
    override val token: KaLifetimeToken get() = backingType.token

    override val type: KaType get() = withValidityAssertion { backingType }

    override val kind: KaImplicitReceiverSmartCastKind get() = withValidityAssertion { backingKind }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaFirImplicitReceiverSmartCast &&
                other.backingType == backingType &&
                other.backingKind == backingKind
    }

    override fun hashCode(): Int {
        return Objects.hash(backingType, backingKind)
    }
}
