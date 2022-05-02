/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.psi.KtArrayAccessExpression

class KtFirArrayAccessReference(
    expression: KtArrayAccessExpression
) : KtArrayAccessReference(expression), KtFirReference {
    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        check(this is KtFirAnalysisSession)
        val fir = element.getOrBuildFirSafe<FirFunctionCall>(firResolveSession) ?: return emptyList()
        return fir.getCandidateSymbols().map { it.fir.buildSymbol(firSymbolBuilder) }
    }
}
