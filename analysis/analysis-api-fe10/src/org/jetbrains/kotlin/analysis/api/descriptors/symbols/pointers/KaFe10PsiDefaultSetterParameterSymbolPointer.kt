/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

class KaFe10PsiDefaultSetterParameterSymbolPointer(
    private val propertySymbolPointer: KaPsiBasedSymbolPointer<KaPropertySetterSymbol>,
) : KaSymbolPointer<KaValueParameterSymbol>() {
    @KaImplementationDetail
    override fun restoreSymbol(analysisSession: KaSession): KaValueParameterSymbol? {
        val property = with(analysisSession) { propertySymbolPointer.restoreSymbol() }
        return property?.parameter
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFe10PsiDefaultSetterParameterSymbolPointer &&
            other.propertySymbolPointer.pointsToTheSameSymbolAs(propertySymbolPointer)
}
