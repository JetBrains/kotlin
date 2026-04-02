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
class KaBaseDelegatedPropertyCallResolutionAttempt(
    private val backingValueGetterCallAttempt: KaSingleCallResolutionAttempt,
    private val backingValueSetterCallAttempt: KaSingleCallResolutionAttempt?,
    private val backingProvideDelegateCallAttempt: KaSingleCallResolutionAttempt?,
) : KaDelegatedPropertyCallResolutionAttempt {
    override val token: KaLifetimeToken get() = backingValueGetterCallAttempt.token

    @Suppress("UNCHECKED_CAST")
    override val call: KaDelegatedPropertyCall?
        get() = withValidityAssertion(fun(): KaBaseDelegatedPropertyCall? {
            val getter = backingValueGetterCallAttempt as? KaCallResolutionSuccess ?: return null
            val setter = backingValueSetterCallAttempt?.let {
                it as? KaCallResolutionSuccess ?: return null
            }

            val provideDelegate = backingProvideDelegateCallAttempt?.let {
                it as? KaCallResolutionSuccess ?: return null
            }

            return KaBaseDelegatedPropertyCall(
                getter.call as KaFunctionCall<KaNamedFunctionSymbol>,
                setter?.call as KaFunctionCall<KaNamedFunctionSymbol>?,
                provideDelegate?.call as KaFunctionCall<KaNamedFunctionSymbol>?,
            )
        })

    override val valueGetterCallAttempt: KaSingleCallResolutionAttempt get() = withValidityAssertion { backingValueGetterCallAttempt }
    override val valueSetterCallAttempt: KaSingleCallResolutionAttempt? get() = withValidityAssertion { backingValueSetterCallAttempt }
    override val provideDelegateCallAttempt: KaSingleCallResolutionAttempt? get() = withValidityAssertion { backingProvideDelegateCallAttempt }
    override val attempts: List<KaSingleCallResolutionAttempt>
        get() = withValidityAssertion {
            listOfNotNull(
                backingValueGetterCallAttempt,
                backingValueSetterCallAttempt,
                backingProvideDelegateCallAttempt,
            )
        }
}
