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
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaScriptSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirScript

internal class KaFirResultPropertySymbolPointer(
    private val scriptPointer: KaSymbolPointer<KaScriptSymbol>,
    originalSymbol: KaKotlinPropertySymbol?,
) : KaBaseCachedSymbolPointer<KaKotlinPropertySymbol>(originalSymbol) {
    @KaImplementationDetail
    override fun restoreIfNotCached(analysisSession: KaSession): KaKotlinPropertySymbol? {
        require(analysisSession is KaFirSession)
        val script = with(analysisSession) {
            scriptPointer.restoreSymbol()?.firSymbol?.fir as? FirScript
        } ?: return null

        @OptIn(DirectDeclarationsAccess::class)
        val lastProperty = script.declarations.lastOrNull() as? FirProperty ?: return null
        if (lastProperty.origin !is FirDeclarationOrigin.ScriptCustomization.ResultProperty) return null
        return analysisSession.firSymbolBuilder.variableBuilder.buildPropertySymbol(lastProperty.symbol) as? KaKotlinPropertySymbol
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirResultPropertySymbolPointer &&
            other.scriptPointer.pointsToTheSameSymbolAs(scriptPointer)
}
