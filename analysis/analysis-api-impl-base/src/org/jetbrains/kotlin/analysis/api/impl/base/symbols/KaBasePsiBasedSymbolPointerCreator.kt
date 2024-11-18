/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KaPsiBasedSymbolPointerCreator
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.psi.KtElement
import kotlin.reflect.KClass

class KaBasePsiBasedSymbolPointerCreator : KaPsiBasedSymbolPointerCreator() {
    @KaExperimentalApi
    override fun symbolPointer(element: KtElement): KaSymbolPointer<KaSymbol> = KaPsiBasedSymbolPointer(element, KaSymbol::class)

    @KaExperimentalApi
    override fun <S : KaSymbol> symbolPointerOfType(
        element: KtElement,
        expectedType: KClass<S>
    ): KaSymbolPointer<S> = KaPsiBasedSymbolPointer(element, expectedType)
}