/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtScriptSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirScript

internal class KtFirScriptSymbolPointer(private val filePointer: KtSymbolPointer<KtFileSymbol>) : KtSymbolPointer<KtScriptSymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtScriptSymbol? {
        require(analysisSession is KtFirAnalysisSession)
        val file = with(analysisSession) {
            filePointer.restoreSymbol()?.firSymbol?.fir as? FirFile
        } ?: return null

        val script = file.declarations.singleOrNull() as? FirScript ?: return null
        return analysisSession.firSymbolBuilder.buildScriptSymbol(script.symbol)
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtFirScriptSymbolPointer &&
            other.filePointer.pointsToTheSameSymbolAs(filePointer)
}
