/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

internal class KaFirBackingFieldSymbolPointer(
    private val propertySymbolPointer: KaSymbolPointer<KaKotlinPropertySymbol>,
) : KaSymbolPointer<KaBackingFieldSymbol>() {
    @KaImplementationDetail
    override fun restoreSymbol(analysisSession: KaSession): KaBackingFieldSymbol? {
        require(analysisSession is KaFirSession)
        val propertySymbol = with(analysisSession) {
            propertySymbolPointer.restoreSymbol()
        } ?: return null

        check(propertySymbol is KaFirKotlinPropertySymbol)
        val backingFieldSymbol = propertySymbol.firSymbol.backingFieldSymbol ?: return null
        return analysisSession.firSymbolBuilder.variableBuilder.buildBackingFieldSymbol(backingFieldSymbol)
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirBackingFieldSymbolPointer &&
            other.propertySymbolPointer.pointsToTheSameSymbolAs(propertySymbolPointer)
}
