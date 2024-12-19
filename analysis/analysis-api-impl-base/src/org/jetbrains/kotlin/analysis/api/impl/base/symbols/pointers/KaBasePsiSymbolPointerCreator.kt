/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers

import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiSymbolPointerCreator
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.psi.KtElement
import kotlin.reflect.KClass

internal class KaBasePsiSymbolPointerCreator : KaPsiSymbolPointerCreator {
    override fun symbolPointer(element: KtElement, originalSymbol: KaSymbol?): KaSymbolPointer<KaSymbol> =
        KaBasePsiSymbolPointer(element, KaSymbol::class, originalSymbol)

    override fun <S : KaSymbol> symbolPointerOfType(
        element: KtElement,
        expectedType: KClass<S>,
        originalSymbol: S?
    ): KaSymbolPointer<S> =
        KaBasePsiSymbolPointer(element, expectedType, originalSymbol)
}