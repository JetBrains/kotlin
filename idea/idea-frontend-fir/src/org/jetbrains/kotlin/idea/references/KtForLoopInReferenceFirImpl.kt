/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.idea.fir.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.fir.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.fir.getResolvedSymbolOfNameReference
import org.jetbrains.kotlin.idea.frontend.api.fir.AnalysisSessionFirImpl
import org.jetbrains.kotlin.psi.KtForExpression

open class KtForLoopInReferenceFirImpl(expression: KtForExpression) : KtForLoopInReference(expression), FirKtReference {
    override fun getResolvedToPsi(
        analysisSession: AnalysisSessionFirImpl,
        session: FirSession,
        state: FirModuleResolveState
    ): Collection<PsiElement> {
        val firLoop = expression.getOrBuildFirSafe<FirWhileLoop>(state) ?: return emptyList()
        val condition = firLoop.condition as? FirFunctionCall
        val iterator = run {
            val callee = (condition?.explicitReceiver as? FirQualifiedAccessExpression)?.calleeReference
            (callee?.getResolvedSymbolOfNameReference()?.fir as? FirProperty)?.getInitializerFunctionCall()
        }
        val hasNext = condition?.calleeReference?.getResolvedSymbolOfNameReference()
        val next = (firLoop.block.statements.firstOrNull() as? FirProperty?)?.getInitializerFunctionCall()
        return listOfNotNull(
            iterator?.fir?.findPsi(session),
            hasNext?.fir?.findPsi(session),
            next?.fir?.findPsi(session),
        )
    }

    private fun FirProperty.getInitializerFunctionCall() =
        (initializer as? FirFunctionCall)?.calleeReference?.getResolvedSymbolOfNameReference()
}