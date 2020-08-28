/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.idea.fir.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.fir.getResolvedKtSymbolOfNameReference
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtConstructorDelegationReferenceExpression

class KtFirConstructorDelegationReference(
    expression: KtConstructorDelegationReferenceExpression
) : KtConstructorDelegationReference(expression), KtFirReference {

    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        check(this is KtFirAnalysisSession)
        val fir = expression.getOrBuildFirSafe<FirDelegatedConstructorCall>(firResolveState) ?: return emptyList()
        return listOfNotNull(fir.calleeReference.getResolvedKtSymbolOfNameReference(firSymbolBuilder))
    }
}
