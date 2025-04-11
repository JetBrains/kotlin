/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.idea.references.KtArrayAccessReference
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtImportAlias

internal class KaFirArrayAccessReference(
    expression: KtArrayAccessExpression,
) : KtArrayAccessReference(expression), KaFirReference {
    override fun KaFirSession.computeSymbols(): Collection<KaSymbol> {
        val fir = element.getOrBuildFir(resolutionFacade) ?: return emptyList()
        return when (fir) {
            is FirFunctionCall -> fir.getCandidateSymbols().map { it.fir.buildSymbol(firSymbolBuilder) }
            is FirResolvedNamedReference -> listOf(fir.resolvedSymbol.buildSymbol(firSymbolBuilder))
            else -> emptyList()
        }
    }

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KaFirReference>.isReferenceToImportAlias(alias)
    }

}
