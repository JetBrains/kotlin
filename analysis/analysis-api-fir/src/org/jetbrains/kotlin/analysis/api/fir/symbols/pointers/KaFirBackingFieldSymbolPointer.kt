/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

internal class KaFirBackingFieldSymbolPointer(
    private val propertySymbolPointer: KaSymbolPointer<KaPropertySymbol>,
    originalSymbol: KaBackingFieldSymbol
) : KaFirCachedSymbolPointer<KaBackingFieldSymbol>(originalSymbol) {
    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean {
        return this === other
                || (other is KaFirBackingFieldSymbolPointer && other.propertySymbolPointer.pointsToTheSameSymbolAs(propertySymbolPointer))
    }

    override fun restoreIfNotCached(analysisSession: KaSession): KaBackingFieldSymbol? = with(analysisSession) {
        val propertySymbol = propertySymbolPointer.restoreSymbol() ?: return null
        propertySymbol.backingFieldSymbol
    }
}