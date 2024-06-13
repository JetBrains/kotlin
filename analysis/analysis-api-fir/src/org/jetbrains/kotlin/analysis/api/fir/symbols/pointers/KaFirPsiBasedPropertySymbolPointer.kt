/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

class KaFirPsiBasedPropertySymbolPointer(
    private val variableSymbolPointer: KaPsiBasedSymbolPointer<KaVariableLikeSymbol>,
) : KaSymbolPointer<KaKotlinPropertySymbol>() {
    @KaImplementationDetail
    override fun restoreSymbol(analysisSession: KaSession): KaKotlinPropertySymbol? =
        when (val variable = with(analysisSession) { variableSymbolPointer.restoreSymbol() }) {
            is KaKotlinPropertySymbol -> variable
            is KaValueParameterSymbol -> variable.generatedPrimaryConstructorProperty
            else -> null
        }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirPsiBasedPropertySymbolPointer &&
            other.variableSymbolPointer.pointsToTheSameSymbolAs(variableSymbolPointer)
}
