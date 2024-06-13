/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithTypeParameters
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.name.Name

internal class KaFirTypeParameterSymbolPointer(
    private val ownerPointer: KaSymbolPointer<KaSymbolWithTypeParameters>,
    private val name: Name,
    private val index: Int,
) : KaSymbolPointer<KaTypeParameterSymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KaSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KaSession): KaTypeParameterSymbol? {
        require(analysisSession is KaFirSession)
        val ownerSymbol = with(analysisSession) {
            ownerPointer.restoreSymbol() ?: return null
        }

        val firTypeParameterSymbol = ownerSymbol.firSymbol.typeParameterSymbols?.getOrNull(index)?.takeIf { it.name == name } ?: return null
        return analysisSession.firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(firTypeParameterSymbol)
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirTypeParameterSymbolPointer &&
            other.index == index &&
            other.name == name &&
            other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}
