/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaSingleSymbolResolutionSuccess
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol

@KaImplementationDetail
class KaBaseSingleSymbolResolutionSuccess(private val backingSymbol: KaSymbol) : KaSingleSymbolResolutionSuccess {
    override val symbol: KaSymbol get() = withValidityAssertion { backingSymbol }
    override val token: KaLifetimeToken get() = backingSymbol.token
}
