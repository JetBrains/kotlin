/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirEnumEntryInitializerSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.restoreSymbol

internal class KaFirEnumEntryInitializerSymbolPointer(
    private val ownerPointer: KaSymbolPointer<KaFirEnumEntrySymbol>,
    originalSymbol: KaFirEnumEntryInitializerSymbol?,
) : KaFirCachedSymbolPointer<KaFirEnumEntryInitializerSymbol>(originalSymbol) {
    @KaImplementationDetail
    context(session: KaSession)
    override fun restoreIfNotCached(): KaFirEnumEntryInitializerSymbol? {
        require(session is KaFirSession)
        val owner = ownerPointer.restoreSymbol()
        return owner?.initializer as? KaFirEnumEntryInitializerSymbol
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirEnumEntryInitializerSymbolPointer &&
            other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}
