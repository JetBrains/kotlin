/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.idea.fir.getCalleeSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.buildSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtArrayAccessExpression

class KtFirArrayAccessReference(
    expression: KtArrayAccessExpression
) : KtArrayAccessReference(expression), KtFirReference {
    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        check(this is KtFirAnalysisSession)
        val fir = element.getOrBuildFirSafe<FirFunctionCall>(firResolveState) ?: return emptyList()
        return listOfNotNull(fir.getCalleeSymbol()?.fir?.buildSymbol(firSymbolBuilder))
    }

    override fun handleElementRename(newElementName: String): PsiElement = TODO("Not yet implemented")
}