/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaDelegatedPropertyCall
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSingleCall
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol

@KaImplementationDetail
class KaBaseDelegatedPropertyCall(
    private val backingValueGetterCall: KaFunctionCall<KaNamedFunctionSymbol>,
    private val backingValueSetterCall: KaFunctionCall<KaNamedFunctionSymbol>?,
    private val backingProvideDelegateCall: KaFunctionCall<KaNamedFunctionSymbol>?,
) : KaDelegatedPropertyCall {
    override val token: KaLifetimeToken get() = backingValueGetterCall.token

    override val valueGetterCall: KaFunctionCall<KaNamedFunctionSymbol>
        get() = withValidityAssertion { backingValueGetterCall }

    override val valueSetterCall: KaFunctionCall<KaNamedFunctionSymbol>?
        get() = withValidityAssertion { backingValueSetterCall }

    override val provideDelegateCall: KaFunctionCall<KaNamedFunctionSymbol>?
        get() = withValidityAssertion { backingProvideDelegateCall }

    @KaExperimentalApi
    override val calls: List<KaSingleCall<*, *>>
        get() = withValidityAssertion {
            listOfNotNull(backingValueGetterCall, backingValueSetterCall, backingProvideDelegateCall)
        }
}
