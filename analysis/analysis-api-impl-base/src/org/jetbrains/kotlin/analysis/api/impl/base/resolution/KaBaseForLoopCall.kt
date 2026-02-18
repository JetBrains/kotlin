/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaForLoopCall
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSingleCall
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol

@KaImplementationDetail
class KaBaseForLoopCall(
    private val backingIteratorCall: KaFunctionCall<KaNamedFunctionSymbol>,
    private val backingHasNextCall: KaFunctionCall<KaNamedFunctionSymbol>,
    private val backingNextCall: KaFunctionCall<KaNamedFunctionSymbol>,
) : KaForLoopCall {
    override val token: KaLifetimeToken get() = backingIteratorCall.token

    override val iteratorCall: KaFunctionCall<KaNamedFunctionSymbol>
        get() = withValidityAssertion { backingIteratorCall }

    override val hasNextCall: KaFunctionCall<KaNamedFunctionSymbol>
        get() = withValidityAssertion { backingHasNextCall }

    override val nextCall: KaFunctionCall<KaNamedFunctionSymbol>
        get() = withValidityAssertion { backingNextCall }

    @KaExperimentalApi
    override val calls: List<KaSingleCall<*, *>>
        get() = withValidityAssertion { listOf(backingIteratorCall, backingHasNextCall, backingNextCall) }
}
