/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.components.KaSymbolDeclarationOverridesProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol

abstract class KaSymbolDeclarationOverridesProviderBase : KaSymbolDeclarationOverridesProvider() {
    protected fun KaValueParameterSymbol.getAllOverriddenSymbols(): List<KaCallableSymbol> {
        generatedPrimaryConstructorProperty?.let { return getAllOverriddenSymbols(it) }
        return emptyList()
    }

    protected fun KaValueParameterSymbol.getDirectlyOverriddenSymbols(): List<KaCallableSymbol> {
        generatedPrimaryConstructorProperty?.let { return getDirectlyOverriddenSymbols(it) }
        return emptyList()
    }
}