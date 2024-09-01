/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol

@KaImplementationDetail
abstract class AbstractKaSymbolDeclarationOverridesProvider<T : KaSession> : KaSessionComponent<T>() {
    protected fun getDirectlyOverriddenSymbolsForParameter(symbol: KaValueParameterSymbol): Sequence<KaCallableSymbol> {
        symbol.generatedPrimaryConstructorProperty?.let {
            return with(analysisSession) { it.directlyOverriddenSymbols }
        }
        return emptySequence()
    }

    protected fun getAllOverriddenSymbolsForParameter(symbol: KaValueParameterSymbol): Sequence<KaCallableSymbol> {
        symbol.generatedPrimaryConstructorProperty?.let {
            return with(analysisSession) { it.allOverriddenSymbols }
        }
        return emptySequence()
    }
}