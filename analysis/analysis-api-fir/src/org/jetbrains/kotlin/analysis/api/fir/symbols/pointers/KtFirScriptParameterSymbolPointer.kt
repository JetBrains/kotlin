/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtScriptSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.name.Name

internal class KtFirScriptParameterSymbolPointer(
    private val parameterName: Name,
    private val scriptPointer: KtSymbolPointer<KtScriptSymbol>,
) : KtSymbolPointer<KtLocalVariableSymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): KtLocalVariableSymbol? {
        require(analysisSession is KtFirAnalysisSession)
        val script = with(analysisSession) {
            scriptPointer.restoreSymbol()?.firSymbol?.fir as? FirScript
        } ?: return null

        val parameter = script.parameters.find { it.name == parameterName } ?: return null
        return analysisSession.firSymbolBuilder.variableLikeBuilder.buildLocalVariableSymbol(parameter.symbol)
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtFirScriptParameterSymbolPointer &&
            other.scriptPointer.pointsToTheSameSymbolAs(scriptPointer)
}
