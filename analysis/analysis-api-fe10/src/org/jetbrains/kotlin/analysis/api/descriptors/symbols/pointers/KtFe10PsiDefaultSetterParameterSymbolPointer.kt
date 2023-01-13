/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer

class KtFe10PsiDefaultSetterParameterSymbolPointer(
    private val propertySymbolPointer: KtPsiBasedSymbolPointer<KtPropertySetterSymbol>,
) : KtSymbolPointer<KtValueParameterSymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtValueParameterSymbol? {
        val property = with(analysisSession) { propertySymbolPointer.restoreSymbol() }
        return property?.parameter
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtFe10PsiDefaultSetterParameterSymbolPointer &&
            other.propertySymbolPointer.pointsToTheSameSymbolAs(propertySymbolPointer)
}
