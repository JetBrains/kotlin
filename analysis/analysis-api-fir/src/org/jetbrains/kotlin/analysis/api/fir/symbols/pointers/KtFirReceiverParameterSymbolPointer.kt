/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer

internal class KtFirReceiverParameterSymbolPointer(
    private val ownerPointer: KtSymbolPointer<KtCallableSymbol>,
) : KtSymbolPointer<KtReceiverParameterSymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtReceiverParameterSymbol? {
        require(analysisSession is KtFirAnalysisSession)
        val callableSymbol = with(analysisSession) {
            ownerPointer.restoreSymbol()
        } ?: return null

        return analysisSession.firSymbolBuilder.callableBuilder.buildExtensionReceiverSymbol(callableSymbol.firSymbol)
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtFirReceiverParameterSymbolPointer &&
            other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}
