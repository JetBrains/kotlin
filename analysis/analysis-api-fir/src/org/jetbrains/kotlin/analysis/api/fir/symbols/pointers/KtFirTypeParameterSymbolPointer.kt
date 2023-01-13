/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithTypeParameters
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.name.Name

internal class KtFirTypeParameterSymbolPointer(
    private val ownerPointer: KtSymbolPointer<KtSymbolWithTypeParameters>,
    private val name: Name,
    private val index: Int,
) : KtSymbolPointer<KtTypeParameterSymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtTypeParameterSymbol? {
        require(analysisSession is KtFirAnalysisSession)
        val ownerSymbol = with(analysisSession) {
            ownerPointer.restoreSymbol() ?: return null
        }

        val firTypeParameterSymbol = ownerSymbol.firSymbol.typeParameterSymbols?.getOrNull(index)?.takeIf { it.name == name } ?: return null
        return analysisSession.firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firTypeParameterSymbol)
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtFirTypeParameterSymbolPointer &&
            other.index == index &&
            other.name == name &&
            other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}
