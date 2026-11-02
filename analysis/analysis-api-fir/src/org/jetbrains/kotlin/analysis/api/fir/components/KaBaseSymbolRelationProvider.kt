/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaSymbolRelationProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol

internal fun <T> T.getDirectlyOverriddenSymbolsForParameter(
    symbol: KaValueParameterSymbol,
): Sequence<KaCallableSymbol> where
        T : KaFirSessionComponent,
        T : KaSymbolRelationProvider = with(analysisSession) {
    symbol.primaryConstructorProperty?.directlyOverriddenSymbols.orEmpty()
}

internal fun <T> T.getAllOverriddenSymbolsForParameter(
    symbol: KaValueParameterSymbol,
): Sequence<KaCallableSymbol> where
        T : KaFirSessionComponent,
        T : KaSymbolRelationProvider = with(analysisSession) {
    symbol.primaryConstructorProperty?.allOverriddenSymbols.orEmpty()
}
