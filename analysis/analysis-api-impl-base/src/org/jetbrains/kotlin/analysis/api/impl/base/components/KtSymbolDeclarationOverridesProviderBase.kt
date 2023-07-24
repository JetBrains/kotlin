/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.components.KtSymbolDeclarationOverridesProvider
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol

abstract class KtSymbolDeclarationOverridesProviderBase : KtSymbolDeclarationOverridesProvider() {
    protected fun KtValueParameterSymbol.getAllOverriddenSymbols(): List<KtCallableSymbol> {
        generatedPrimaryConstructorProperty?.let { return getAllOverriddenSymbols(it) }
        return emptyList()
    }

    protected fun KtValueParameterSymbol.getDirectlyOverriddenSymbols(): List<KtCallableSymbol> {
        generatedPrimaryConstructorProperty?.let { return getDirectlyOverriddenSymbols(it) }
        return emptyList()
    }
}