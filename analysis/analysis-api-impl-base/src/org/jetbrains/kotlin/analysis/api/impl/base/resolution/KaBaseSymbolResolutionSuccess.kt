/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionSuccess
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol

@KaImplementationDetail
class KaBaseSymbolResolutionSuccess(
    private val backingSymbols: List<KaSymbol>,
    override val token: KaLifetimeToken,
) : KaSymbolResolutionSuccess {
    constructor(backingSymbol: KaSymbol) : this(
        backingSymbols = listOf(backingSymbol),
        token = backingSymbol.token,
    )

    override val symbols: List<KaSymbol> get() = withValidityAssertion { backingSymbols }
}
