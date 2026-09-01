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
    private val backingValueGetterCallAttempt: KaSimpleCallResolutionAttempt,
    private val backingValueSetterCallAttempt: KaSimpleCallResolutionAttempt?,
    private val backingProvideDelegateCallAttempt: KaSimpleCallResolutionAttempt?,
) : KaDelegatedPropertyCallResolutionAttempt {
    override val token: KaLifetimeToken get() = backingValueGetterCallAttempt.token

    @Suppress("UNCHECKED_CAST")
    override val call: KaDelegatedPropertyCall?
        get() = withValidityAssertion(fun(): KaBaseDelegatedPropertyCall? {
            val getter = backingValueGetterCallAttempt as? KaSimpleCallResolutionSuccess ?: return null
            val setter = backingValueSetterCallAttempt?.let {
                it as? KaSimpleCallResolutionSuccess ?: return null
            }

            val provideDelegate = backingProvideDelegateCallAttempt?.let {
                it as? KaSimpleCallResolutionSuccess ?: return null
            }

            return KaBaseDelegatedPropertyCall(
                getter.call as KaFunctionCall<KaNamedFunctionSymbol>,
                setter?.call as KaFunctionCall<KaNamedFunctionSymbol>?,
                provideDelegate?.call as KaFunctionCall<KaNamedFunctionSymbol>?,
            )
        })

    override val valueGetterCallAttempt: KaSimpleCallResolutionAttempt get() = withValidityAssertion { backingValueGetterCallAttempt }
    override val valueSetterCallAttempt: KaSimpleCallResolutionAttempt? get() = withValidityAssertion { backingValueSetterCallAttempt }
    override val provideDelegateCallAttempt: KaSimpleCallResolutionAttempt? get() = withValidityAssertion { backingProvideDelegateCallAttempt }
    override val simpleAttempts: List<KaSimpleCallResolutionAttempt>
        get() = withValidityAssertion {
            listOfNotNull(
                backingValueGetterCallAttempt,
                backingValueSetterCallAttempt,
                backingProvideDelegateCallAttempt,
            )
        }

    @Deprecated("Use 'simpleAttempts' instead", ReplaceWith("simpleAttempts"))
    override val attempts: List<KaSimpleCallResolutionAttempt> get() = simpleAttempts
}
