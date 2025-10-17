/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaSmartCastedReceiverValue
import org.jetbrains.kotlin.analysis.api.types.KaType

@KaImplementationDetail
class KaBaseSmartCastedReceiverValue(
    private val backingOriginal: KaReceiverValue,
    smartCastType: KaType,
) : KaSmartCastedReceiverValue {
    private val backingType: KaType = smartCastType
    override val token: KaLifetimeToken get() = backingOriginal.token

    override val original: KaReceiverValue get() = withValidityAssertion { backingOriginal }
    override val type: KaType get() = withValidityAssertion { backingType }
}
