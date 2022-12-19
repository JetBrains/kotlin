/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer

class KtFirPsiBasedPropertySymbolPointer(
    private val variableSymbolPointer: KtPsiBasedSymbolPointer<KtVariableLikeSymbol>,
) : KtSymbolPointer<KtKotlinPropertySymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtKotlinPropertySymbol? =
        when (val variable = with(analysisSession) { variableSymbolPointer.restoreSymbol() }) {
            is KtKotlinPropertySymbol -> variable
            is KtValueParameterSymbol -> variable.generatedPrimaryConstructorProperty
            else -> null
        }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtFirPsiBasedPropertySymbolPointer &&
            other.variableSymbolPointer.pointsToTheSameSymbolAs(variableSymbolPointer)
}
