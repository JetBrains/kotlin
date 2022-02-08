/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer

internal class KtFirBackingFieldSymbolPointer(
    private val propertySymbolPointer: KtSymbolPointer<KtKotlinPropertySymbol>,
) : KtSymbolPointer<KtBackingFieldSymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtBackingFieldSymbol? {
        require(analysisSession is KtFirAnalysisSession)
        @Suppress("DEPRECATION")
        val propertySymbol = propertySymbolPointer.restoreSymbol(analysisSession) ?: return null
        check(propertySymbol is KtFirKotlinPropertySymbol)
        return analysisSession.firSymbolBuilder.variableLikeBuilder.buildBackingFieldSymbolByProperty(propertySymbol.firSymbol)
    }
}

