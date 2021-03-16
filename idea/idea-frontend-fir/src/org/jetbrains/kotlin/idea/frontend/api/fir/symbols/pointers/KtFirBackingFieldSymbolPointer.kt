/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers

import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirKotlinPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtBackingFieldSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer

internal class KtFirBackingFieldSymbolPointer(
    private val propertySymbolPointer: KtSymbolPointer<KtKotlinPropertySymbol>,
) : KtSymbolPointer<KtBackingFieldSymbol>() {
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtBackingFieldSymbol? {
        require(analysisSession is KtFirAnalysisSession)
        val propertySymbol = propertySymbolPointer.restoreSymbol(analysisSession) ?: return null
        check(propertySymbol is KtFirKotlinPropertySymbol)
        return propertySymbol.firRef.withFir { firProperty ->
            analysisSession.firSymbolBuilder.variableLikeBuilder.buildBackingFieldSymbolByProperty(firProperty)
        }
    }
}

