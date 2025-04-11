/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.getResolvedSymbolOfNameReference
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.idea.references.KtForLoopInReference
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtImportAlias

internal class KaFirForLoopInReference(expression: KtForExpression) : KtForLoopInReference(expression), KaFirReference {
    override fun KaFirSession.computeSymbols(): Collection<KaSymbol> {
        val firLoop = expression.getOrBuildFirSafe<FirWhileLoop>(resolutionFacade) ?: return emptyList()
        val condition = firLoop.condition as? FirFunctionCall
        val iterator = this@KaFirForLoopInReference.run {
            val callee = (condition?.explicitReceiver as? FirQualifiedAccessExpression)?.calleeReference
            (callee?.getResolvedSymbolOfNameReference()?.fir as? FirProperty)?.getInitializerFunctionCall()
        }
        val hasNext = condition?.calleeReference?.getResolvedSymbolOfNameReference()
        val next = (firLoop.block.statements.firstOrNull() as? FirProperty?)?.getInitializerFunctionCall()
        return listOfNotNull(
            iterator?.fir?.buildSymbol(firSymbolBuilder),
            hasNext?.fir?.buildSymbol(firSymbolBuilder),
            next?.fir?.buildSymbol(firSymbolBuilder),
        )
    }

    private fun FirProperty.getInitializerFunctionCall() =
        (initializer as? FirFunctionCall)?.calleeReference?.getResolvedSymbolOfNameReference()

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KaFirReference>.isReferenceToImportAlias(alias)
    }

}