/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.getResolvedKtSymbolOfNameReference
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.idea.references.KtConstructorDelegationReference
import org.jetbrains.kotlin.psi.KtConstructorDelegationReferenceExpression
import org.jetbrains.kotlin.psi.KtImportAlias

internal class KaFirConstructorDelegationReference(
    expression: KtConstructorDelegationReferenceExpression,
) : KtConstructorDelegationReference(expression), KaFirReference {

    override fun KaFirSession.computeSymbols(): Collection<KaSymbol> {
        val fir = expression.getOrBuildFirSafe<FirDelegatedConstructorCall>(resolutionFacade) ?: return emptyList()
        return listOfNotNull(fir.calleeReference.getResolvedKtSymbolOfNameReference(firSymbolBuilder))
    }

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KaFirReference>.isReferenceToImportAlias(alias)
    }
}
