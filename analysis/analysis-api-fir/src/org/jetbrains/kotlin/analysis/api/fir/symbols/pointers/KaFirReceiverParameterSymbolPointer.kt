/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.restoreSymbol

internal class KaFirReceiverParameterSymbolPointer(
    private val ownerPointer: KaSymbolPointer<KaCallableSymbol>,
    originalSymbol: KaReceiverParameterSymbol?,
) : KaFirCachedSymbolPointer<KaReceiverParameterSymbol>(originalSymbol) {
    @KaImplementationDetail
    context(session: KaSession)
    override fun restoreIfNotCached(): KaReceiverParameterSymbol? {
        val callableSymbol = ownerPointer.restoreSymbol()

        return callableSymbol?.receiverParameter
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirReceiverParameterSymbolPointer &&
            other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}
