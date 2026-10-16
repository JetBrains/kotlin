/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

@KaImplementationDetail
class KaBaseReceiverParameterSymbolPointer(
    private val ownerPointer: KaSymbolPointer<KaCallableSymbol>,
    originalSymbol: KaReceiverParameterSymbol?,
) : KaBaseCachedSymbolPointer<KaReceiverParameterSymbol>(originalSymbol) {
    @KaImplementationDetail
    override fun restoreIfNotCached(analysisSession: KaSession): KaReceiverParameterSymbol? {
        val callableSymbol = with(analysisSession) {
            ownerPointer.restoreSymbol()
        }

        return callableSymbol?.receiverParameter
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaBaseReceiverParameterSymbolPointer &&
            other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}
