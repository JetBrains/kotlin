/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer

internal class KtFirPropertyAccessorSymbolPointer(
    private val propertySymbolPointer: KtSymbolPointer<KtKotlinPropertySymbol>,
    private val isGetter: Boolean,
) : KtSymbolPointer<KtPropertyAccessorSymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtPropertyAccessorSymbol? {
        require(analysisSession is KtFirAnalysisSession)
        val propertySymbol = with(analysisSession) {
            propertySymbolPointer.restoreSymbol()
        } ?: return null

        check(propertySymbol is KtFirKotlinPropertySymbol)
        return if (isGetter) propertySymbol.getter else propertySymbol.setter
    }
}
