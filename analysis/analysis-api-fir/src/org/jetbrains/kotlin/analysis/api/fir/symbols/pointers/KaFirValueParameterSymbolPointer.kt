/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.name.Name

internal class KaFirValueParameterSymbolPointer(
    private val ownerPointer: KaSymbolPointer<KaFunctionLikeSymbol>,
    private val name: Name,
    private val index: Int,
) : KaSymbolPointer<KaValueParameterSymbol>() {
    @KaImplementationDetail
    override fun restoreSymbol(analysisSession: KaSession): KaValueParameterSymbol? {
        require(analysisSession is KaFirSession)
        val ownerSymbol = with(analysisSession) {
            ownerPointer.restoreSymbol() ?: return null
        }

        val function = ownerSymbol.firSymbol.fir as? FirFunction ?: return null
        val firValueParameterSymbol = function.valueParameters.getOrNull(index)?.symbol?.takeIf { it.name == name } ?: return null
        return analysisSession.firSymbolBuilder.variableLikeBuilder.buildValueParameterSymbol(firValueParameterSymbol)
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirValueParameterSymbolPointer &&
            other.index == index &&
            other.name == name &&
            other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}
