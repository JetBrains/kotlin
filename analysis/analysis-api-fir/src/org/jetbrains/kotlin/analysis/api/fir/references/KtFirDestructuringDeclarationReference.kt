/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.analysis.api.fir.getCalleeSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry

class KtFirDestructuringDeclarationReference(
    element: KtDestructuringDeclarationEntry
) : KtDestructuringDeclarationReference(element), KtFirReference {
    override fun canRename(): Boolean = false //todo

    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        check(this is KtFirAnalysisSession)
        val fir = expression.getOrBuildFirSafe<FirProperty>(firResolveSession) ?: return emptyList()
        return listOfNotNull(
            fir.buildSymbol(firSymbolBuilder),
            getComponentNSymbol(fir)
        )
    }

    private fun KtFirAnalysisSession.getComponentNSymbol(fir: FirProperty): KtSymbol? {
        val componentFunctionSymbol = (fir.initializer as? FirComponentCall)?.getCalleeSymbol() ?: return null
        return componentFunctionSymbol.fir.buildSymbol(firSymbolBuilder)
    }
}
