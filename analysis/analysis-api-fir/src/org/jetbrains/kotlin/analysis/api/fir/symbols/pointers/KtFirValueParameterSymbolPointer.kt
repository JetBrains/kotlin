/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.name.Name

internal class KtFirValueParameterSymbolPointer(
    private val ownerPointer: KtSymbolPointer<KtFunctionLikeSymbol>,
    private val name: Name,
    private val index: Int,
) : KtSymbolPointer<KtValueParameterSymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtValueParameterSymbol? {
        require(analysisSession is KtFirAnalysisSession)
        val ownerSymbol = with(analysisSession) {
            ownerPointer.restoreSymbol() ?: return null
        }

        val function = ownerSymbol.firSymbol.fir as? FirFunction ?: return null
        val firValueParameterSymbol = function.valueParameters.getOrNull(index)?.symbol?.takeIf { it.name == name } ?: return null
        return analysisSession.firSymbolBuilder.variableLikeBuilder.buildValueParameterSymbol(firValueParameterSymbol)
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtFirValueParameterSymbolPointer &&
            other.index == index &&
            other.name == name &&
            other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}
