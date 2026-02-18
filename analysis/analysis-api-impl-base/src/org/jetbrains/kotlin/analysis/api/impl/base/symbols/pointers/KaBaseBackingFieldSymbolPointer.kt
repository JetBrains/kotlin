/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

@KaImplementationDetail
class KaBaseBackingFieldSymbolPointer(
    private val propertySymbolPointer: KaSymbolPointer<KaPropertySymbol>,
    originalSymbol: KaBackingFieldSymbol
) : KaBaseCachedSymbolPointer<KaBackingFieldSymbol>(originalSymbol) {
    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean {
        return this === other
                || (other is KaBaseBackingFieldSymbolPointer && other.propertySymbolPointer.pointsToTheSameSymbolAs(propertySymbolPointer))
    }

    override fun restoreIfNotCached(analysisSession: KaSession): KaBackingFieldSymbol? = with(analysisSession) {
        val propertySymbol = propertySymbolPointer.restoreSymbol() ?: return null
        propertySymbol.backingFieldSymbol
    }
}