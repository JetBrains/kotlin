/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseCachedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaScriptSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirScript

internal class KaFirScriptSymbolPointer(
    private val filePointer: KaSymbolPointer<KaFileSymbol>,
    originalSymbol: KaScriptSymbol?,
) : KaBaseCachedSymbolPointer<KaScriptSymbol>(originalSymbol) {
    @KaImplementationDetail
    override fun restoreIfNotCached(analysisSession: KaSession): KaScriptSymbol? {
        require(analysisSession is KaFirSession)
        val file = with(analysisSession) {
            filePointer.restoreSymbol()?.firSymbol?.fir as? FirFile
        } ?: return null

        val script = file.declarations.singleOrNull() as? FirScript ?: return null
        return analysisSession.firSymbolBuilder.buildScriptSymbol(script.symbol)
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirScriptSymbolPointer &&
            other.filePointer.pointsToTheSameSymbolAs(filePointer)
}
