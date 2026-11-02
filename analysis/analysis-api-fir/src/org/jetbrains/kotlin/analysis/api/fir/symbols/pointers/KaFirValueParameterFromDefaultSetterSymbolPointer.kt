/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

@KaImplementationDetail
class KaFirValueParameterFromDefaultSetterSymbolPointer(
    private val ownerPointer: KaSymbolPointer<KaPropertySymbol>,
    originalSymbol: KaValueParameterSymbol?,
) : KaFirCachedSymbolPointer<KaValueParameterSymbol>(originalSymbol) {
    @KaImplementationDetail
    override fun restoreIfNotCached(analysisSession: KaSession): KaValueParameterSymbol? {
        val ownerSymbol = with(analysisSession) {
            ownerPointer.restoreSymbol()
        }

        return ownerSymbol?.setter?.parameter
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirValueParameterFromDefaultSetterSymbolPointer &&
            other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}
