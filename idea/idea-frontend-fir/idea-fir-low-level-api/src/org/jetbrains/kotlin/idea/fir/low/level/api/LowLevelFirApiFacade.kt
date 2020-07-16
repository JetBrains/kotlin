/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.idea.util.getElementTextInContext

object LowLevelFirApiFacade {
    fun getResolveStateFor(element: KtElement): FirModuleResolveState =
        element.firResolveState()

    fun getResolveStateForCompletion(element: KtElement, originalState: FirModuleResolveState): FirModuleResolveState {
        check(originalState is FirModuleResolveStateImpl)
        return FirModuleResolveStateForCompletion(originalState)
    }

    fun getSessionFor(element: KtElement): FirSession =
        getResolveStateFor(element).getSessionFor(element.getModuleInfo())

    fun getOrBuildFirFor(element: KtElement, resolveState: FirModuleResolveState, phase: FirResolvePhase): FirElement =
        resolveState.getOrBuildFirFor(element, phase)

    class FirCompletionContext internal constructor(
        val session: FirSession,
        private val towerDataContextForStatement: Map<FirStatement, FirTowerDataContext>,
        private val state: FirModuleResolveState,
    ) {
        fun getTowerDataContext(element: KtElement): FirTowerDataContext {
            var current: PsiElement? = element
            while (current is KtElement) {
                val mappedFir = state.getCachedMappingForCompletion(current)

                if (mappedFir is FirStatement) {
                    towerDataContextForStatement[mappedFir]?.let { return it }
                    for ((key, value) in towerDataContextForStatement) {
                        // TODO Rework that
                        if (key.psi == mappedFir.psi) {
                            return value
                        }
                    }
                }
                current = current.parent
            }

            error("No context for ${element.getElementTextInContext()}")
        }
    }

    fun buildCompletionContextForFunction(
        firFile: FirFile,
        element: KtNamedFunction,
        state: FirModuleResolveState,
        phase: FirResolvePhase = FirResolvePhase.BODY_RESOLVE
    ): FirCompletionContext {
        val firIdeProvider = firFile.session.firIdeProvider
        val builtFunction = firIdeProvider.buildFunctionWithBody(element)
        val towerDataContextForStatement = mutableMapOf<FirStatement, FirTowerDataContext>()

        val function = builtFunction.apply {
            state.lazyResolveFunctionForCompletion(this, firFile, firIdeProvider, phase, towerDataContextForStatement)
            state.recordPsiToFirMappingsForCompletionFrom(this, firFile, element.containingKtFile)
        }

        return FirCompletionContext(
            function.session,
            towerDataContextForStatement,
            state
        )
    }

    fun getDiagnosticsFor(element: KtElement, resolveState: FirModuleResolveState): Collection<Diagnostic> {
        return resolveState.getDiagnostics(element)
    }
}
