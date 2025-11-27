/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.getCalleeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.idea.references.KtDestructuringDeclarationReference
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtImportAlias

internal class KaFirDestructuringDeclarationReference(
    element: KtDestructuringDeclarationEntry,
) : KtDestructuringDeclarationReference(element), KaFirReference {
    override fun canRename(): Boolean = false //todo

    override fun KaFirSession.computeSymbols(): Collection<KaSymbol> {
        val fir = expression.getOrBuildFirSafe<FirProperty>(resolutionFacade) ?: return emptyList()
        return listOfNotNull(
            // TODO(KT-82708): Only the initializer symbol is expected
            fir.buildSymbol(firSymbolBuilder),
            initializerSymbol(fir),
        )
    }

    private fun KaFirSession.initializerSymbol(fir: FirProperty): KaSymbol? {
        val firSymbol = when (val initializer = fir.initializer) {
            // Positional destructuring
            is FirComponentCall -> initializer.getCalleeSymbol()

            // Name-based destructuring
            is FirPropertyAccessExpression -> initializer.calleeReference.symbol
            else -> null
        } ?: return null

        return firSymbol.fir.buildSymbol(firSymbolBuilder)
    }

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KaFirReference>.isReferenceToImportAlias(alias)
    }
}
