/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSymbolRelationProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol

@KaImplementationDetail
fun <T> T.getDirectlyOverriddenSymbolsForParameter(
    symbol: KaValueParameterSymbol,
): Sequence<KaCallableSymbol> where
        T : KaBaseSessionComponent<out KaSession>,
        T : KaSymbolRelationProvider = with(analysisSession) {
    symbol.generatedPrimaryConstructorProperty?.directlyOverriddenSymbols.orEmpty()
}

@KaImplementationDetail
fun <T> T.getAllOverriddenSymbolsForParameter(
    symbol: KaValueParameterSymbol,
): Sequence<KaCallableSymbol> where
        T : KaBaseSessionComponent<out KaSession>,
        T : KaSymbolRelationProvider = with(analysisSession) {
    symbol.generatedPrimaryConstructorProperty?.allOverriddenSymbols.orEmpty()
}
