/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol

@KaImplementationDetail
class KaBaseForLoopCallResolutionAttempt(
    private val backingIteratorCallAttempt: KaSingleCallResolutionAttempt,
    private val backingHasNextCallAttempt: KaSingleCallResolutionAttempt,
    private val backingNextCallAttempt: KaSingleCallResolutionAttempt,
) : KaForLoopCallResolutionAttempt {
    override val token: KaLifetimeToken get() = backingIteratorCallAttempt.token

    @Suppress("UNCHECKED_CAST")
    override val call: KaForLoopCall?
        get() = withValidityAssertion {
            assembleMultiCall(backingIteratorCallAttempt, backingHasNextCallAttempt, backingNextCallAttempt) { iterator, hasNext, next ->
                KaBaseForLoopCall(
                    iterator.call as KaFunctionCall<KaNamedFunctionSymbol>,
                    hasNext.call as KaFunctionCall<KaNamedFunctionSymbol>,
                    next.call as KaFunctionCall<KaNamedFunctionSymbol>,
                )
            }
        }

    override val iteratorCallAttempt: KaSingleCallResolutionAttempt get() = withValidityAssertion { backingIteratorCallAttempt }
    override val hasNextCallAttempt: KaSingleCallResolutionAttempt get() = withValidityAssertion { backingHasNextCallAttempt }
    override val nextCallAttempt: KaSingleCallResolutionAttempt get() = withValidityAssertion { backingNextCallAttempt }
    override val attempts: List<KaSingleCallResolutionAttempt>
        get() = withValidityAssertion { listOf(backingIteratorCallAttempt, backingHasNextCallAttempt, backingNextCallAttempt) }
}
