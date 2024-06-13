/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaScriptSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.name.Name

internal class KaFirScriptParameterSymbolPointer(
    private val parameterName: Name,
    private val scriptPointer: KaSymbolPointer<KaScriptSymbol>,
) : KaSymbolPointer<KaLocalVariableSymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KaSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KaSession): KaLocalVariableSymbol? {
        require(analysisSession is KaFirSession)
        val script = with(analysisSession) {
            scriptPointer.restoreSymbol()?.firSymbol?.fir as? FirScript
        } ?: return null

        val parameter = script.parameters.find { it.name == parameterName } ?: return null
        return analysisSession.firSymbolBuilder.variableLikeBuilder.buildLocalVariableSymbol(parameter.symbol)
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirScriptParameterSymbolPointer &&
            other.scriptPointer.pointsToTheSameSymbolAs(scriptPointer)
}
