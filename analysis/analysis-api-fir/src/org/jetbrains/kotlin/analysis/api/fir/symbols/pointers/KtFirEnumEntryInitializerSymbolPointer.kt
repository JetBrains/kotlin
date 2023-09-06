/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirEnumEntryInitializerSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer

internal class KtFirEnumEntryInitializerSymbolPointer(
    private val ownerPointer: KtSymbolPointer<KtFirEnumEntrySymbol>,
) : KtSymbolPointer<KtFirEnumEntryInitializerSymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtFirEnumEntryInitializerSymbol? {
        require(analysisSession is KtFirAnalysisSession)
        val owner = with(analysisSession) {
            ownerPointer.restoreSymbol()
        }
        return owner?.enumEntryInitializer
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtFirEnumEntryInitializerSymbolPointer &&
            other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}
