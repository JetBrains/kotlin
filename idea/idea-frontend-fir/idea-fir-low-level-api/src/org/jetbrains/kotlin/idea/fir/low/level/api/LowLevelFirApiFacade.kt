/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
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

    fun buildFunctionWithResolvedBody(
        firFile: FirFile,
        element: KtNamedFunction,
        phase: FirResolvePhase = FirResolvePhase.BODY_RESOLVE
    ): FirFunction<*> {
        val state = element.firResolveState()
        val firIdeProvider = firFile.session.firIdeProvider
        val builtFunction = firIdeProvider.buildFunctionWithBody(element)

        return builtFunction.apply {
            runResolve(firFile, firIdeProvider, phase, state)

            // TODO this PSI caching should be somewhere else
            state.recordElementsFrom(this)
        }
    }

    fun getDiagnosticsFor(element: KtElement, resolveState: FirModuleResolveState): Collection<Diagnostic> {
        val file = element.containingKtFile
        file.getOrBuildFirWithDiagnostics(resolveState)
        return resolveState.getDiagnostics(element)
    }
}