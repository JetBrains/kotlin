/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.idea.fir.*
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.buildSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPropertyDelegate

class KtFirPropertyDelegationMethodsReference(
    element: KtPropertyDelegate
) : KtPropertyDelegationMethodsReference(element), KtFirReference {
    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        check(this is KtFirAnalysisSession)
        val property = (expression.parent as? KtElement)?.getOrBuildFirSafe<FirProperty>(firResolveState) ?: return emptyList()
        if (property.delegate == null) return emptyList()
        val getValueSymbol = (property.getter?.singleStatementOfType<FirReturnExpression>()?.result as? FirFunctionCall)?.getCalleeSymbol()
        val setValueSymbol = property.setter?.singleStatementOfType<FirFunctionCall>()?.getCalleeSymbol()
        return listOfNotNull(
            getValueSymbol?.fir?.buildSymbol(firSymbolBuilder),
            setValueSymbol?.fir?.buildSymbol(firSymbolBuilder)
        )
    }

    private inline fun <reified S : FirStatement> FirPropertyAccessor.singleStatementOfType(): S? =
        body?.statements?.singleOrNull() as? S
}