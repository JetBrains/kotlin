/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirScript

internal class KtFirResultPropertySymbolPointer(private val filePointer: KtSymbolPointer<KtFirFileSymbol>) :
    KtSymbolPointer<KtKotlinPropertySymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtKotlinPropertySymbol? {
        require(analysisSession is KtFirAnalysisSession)
        val file = with(analysisSession) {
            filePointer.restoreSymbol()?.firSymbol
        } ?: return null

        val script = file.fir.declarations.singleOrNull() as? FirScript ?: return null
        val lastProperty = script.statements.lastOrNull() as? FirProperty ?: return null
        if (lastProperty.origin !is FirDeclarationOrigin.ScriptCustomization.ResultProperty) return null
        return analysisSession.firSymbolBuilder.variableLikeBuilder.buildPropertySymbol(lastProperty.symbol) as? KtKotlinPropertySymbol
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtFirResultPropertySymbolPointer &&
            other.filePointer.pointsToTheSameSymbolAs(filePointer)
}
