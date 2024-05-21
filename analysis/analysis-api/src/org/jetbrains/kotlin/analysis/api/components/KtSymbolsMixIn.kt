/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

public interface KaSymbolsMixIn : KaSessionMixIn {
    @Suppress("DEPRECATION")
    public fun <S : KaSymbol> KaSymbolPointer<S>.restoreSymbol(): S? = withValidityAssertion { restoreSymbol(analysisSession) }
}

public typealias KtSymbolsMixIn = KaSymbolsMixIn