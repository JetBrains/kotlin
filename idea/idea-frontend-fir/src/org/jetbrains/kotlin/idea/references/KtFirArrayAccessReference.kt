/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.idea.fir.getCalleeSymbol
import org.jetbrains.kotlin.idea.fir.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.buildSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression

class KtFirArrayAccessReference(
    expression: KtArrayAccessExpression
) : KtArrayAccessReference(expression), KtFirReference {
    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        check(this is KtFirAnalysisSession)
        val fir = element.getOrBuildFirSafe<FirFunctionCall>(firResolveState) ?: return emptyList()
        return listOfNotNull(fir.getCalleeSymbol()?.fir?.buildSymbol(firSymbolBuilder))
    }

    override fun moveFunctionLiteralOutsideParentheses(callExpression: KtCallExpression) {
        TODO("Not yet implemented")
    }

    override fun canMoveLambdaOutsideParentheses(callExpression: KtCallExpression): Boolean {
        TODO("Not yet implemented")
    }

    override fun doRenameImplicitConventionalCall(newName: String?): KtExpression {
        TODO("Not yet implemented")
    }
}