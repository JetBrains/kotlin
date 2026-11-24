/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaScriptSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.restoreSymbol
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.name.Name

internal class KaFirScriptParameterSymbolPointer(
    private val parameterName: Name,
    private val scriptPointer: KaSymbolPointer<KaScriptSymbol>,
    originalSymbol: KaLocalVariableSymbol?,
) : KaFirCachedSymbolPointer<KaLocalVariableSymbol>(originalSymbol) {
    @KaImplementationDetail
    context(session: KaSession)
    override fun restoreIfNotCached(): KaLocalVariableSymbol? {
        require(session is KaFirSession)
        val script = scriptPointer.restoreSymbol()?.firSymbol?.fir as? FirScript ?: return null

        val parameter = script.parameters.find { it.name == parameterName } ?: return null
        return session.firSymbolBuilder.variableBuilder.buildLocalVariableSymbol(parameter.symbol)
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirScriptParameterSymbolPointer &&
            other.scriptPointer.pointsToTheSameSymbolAs(scriptPointer)
}
