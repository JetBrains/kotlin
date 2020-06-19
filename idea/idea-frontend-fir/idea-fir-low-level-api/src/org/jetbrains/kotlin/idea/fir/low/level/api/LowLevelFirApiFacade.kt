/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction

object LowLevelFirApiFacade {
    fun getResolveStateFor(element: KtElement): FirModuleResolveState =
        element.firResolveState()

    fun getResolveStateForCompletion(element: KtElement, mainState: FirModuleResolveStateImpl): FirModuleResolveStateForCompletion {
        return FirModuleResolveStateForCompletion(mainState)
    }

    fun getSessionFor(element: KtElement, resolveState: FirModuleResolveState): FirSession =
        resolveState.getSession(element)

    fun getOrBuildFirFor(element: KtElement, resolveState: FirModuleResolveState, phase: FirResolvePhase): FirElement =
        element.getOrBuildFir(resolveState, phase)


    fun getFirOfClosestParent(element: KtElement): FirElement? = element.getFirOfClosestParent(element.firResolveState())?.second

    class FirCompletionContext internal constructor(
        val session: FirSession,
        private val towerDataContextForStatement: MutableMap<FirStatement, FirTowerDataContext>,
        private val state: FirModuleResolveState,
    ) {
        fun getTowerDataContext(element: KtElement): FirTowerDataContext {
            var current: PsiElement? = element
            while (current is KtElement) {
                val mappedFir = state.getCachedMapping(current)

                if (mappedFir is FirStatement) {
                    towerDataContextForStatement[mappedFir]?.let { return it }
                }
                current = current.parent
            }

            error("No context for $element")
        }
    }

    fun buildCompletionContextForFunction(
        firFile: FirFile,
        element: KtNamedFunction,
        phase: FirResolvePhase = FirResolvePhase.BODY_RESOLVE
    ): FirCompletionContext {
        val state = element.firResolveState()
        val firIdeProvider = firFile.session.firIdeProvider
        val builtFunction = firIdeProvider.buildFunctionWithBody(element)
        val towerDataContextForStatement = mutableMapOf<FirStatement, FirTowerDataContext>()

        val function = builtFunction.apply {
            runResolve(firFile, firIdeProvider, phase, state, towerDataContextForStatement)

            // TODO this PSI caching should be somewhere else
            state.recordElementsFrom(this)
        }

        return FirCompletionContext(
            function.session,
            towerDataContextForStatement,
            state
        )
    }

    fun getDiagnosticsFor(element: KtElement, resolveState: FirModuleResolveState): Collection<Diagnostic> {
        val file = element.containingKtFile
        file.getOrBuildFirWithDiagnostics(resolveState)
        return resolveState.getDiagnostics(element)
    }
}
