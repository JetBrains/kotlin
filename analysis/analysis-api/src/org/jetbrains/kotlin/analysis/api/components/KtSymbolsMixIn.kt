/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer

public interface KtSymbolsMixIn : KtAnalysisSessionMixIn {
    @Suppress("DEPRECATION")
    public fun <S : KtSymbol> KtSymbolPointer<S>.restoreSymbol(): S? = withValidityAssertion { restoreSymbol(analysisSession) }
}