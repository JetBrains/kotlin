/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.idea.fir.getCalleeSymbol
import org.jetbrains.kotlin.idea.fir.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.frontend.api.FrontendAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.FirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.buildSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry

class KtDestructuringDeclarationReferenceFirImpl(
    element: KtDestructuringDeclarationEntry
) : KtDestructuringDeclarationReference(element), FirKtReference {
    override fun canRename(): Boolean = false //todo

    override fun resolveToSymbols(analysisSession: FrontendAnalysisSession): Collection<KtSymbol> {
        check(analysisSession is FirAnalysisSession)
        val fir = expression.getOrBuildFirSafe<FirProperty>() ?: return emptyList()
        val componentFunctionSymbol = (fir.initializer as? FirComponentCall)?.getCalleeSymbol() ?: return emptyList()
        return listOfNotNull(componentFunctionSymbol.fir.buildSymbol(analysisSession.firSymbolBuilder))
    }
}
