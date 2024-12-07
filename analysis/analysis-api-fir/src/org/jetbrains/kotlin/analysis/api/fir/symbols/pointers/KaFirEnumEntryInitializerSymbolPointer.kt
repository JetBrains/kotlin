/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

internal class KaFirEnumEntryInitializerSymbolPointer(
    private val ownerPointer: KaSymbolPointer<KaFirEnumEntrySymbol>,
) : KaSymbolPointer<KaFirEnumEntryInitializerSymbol>() {
    @KaImplementationDetail
    override fun restoreSymbol(analysisSession: KaSession): KaFirEnumEntryInitializerSymbol? {
        require(analysisSession is KaFirSession)
        val owner = with(analysisSession) {
            ownerPointer.restoreSymbol()
        }
        return owner?.enumEntryInitializer
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirEnumEntryInitializerSymbolPointer &&
            other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}
