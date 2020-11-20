/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.FirIdeResolveStateService
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSourcesSession
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import kotlin.reflect.KClass

object LowLevelFirApiFacade {
    fun getResolveStateFor(element: KtElement): FirModuleResolveState =
        getResolveStateFor(element.getModuleInfo())

    fun getResolveStateFor(moduleInfo: IdeaModuleInfo): FirModuleResolveState =
        FirIdeResolveStateService.getInstance(moduleInfo.project!!).getResolveState(moduleInfo)

    fun getSessionFor(element: KtElement): FirSession =
        getResolveStateFor(element).getSessionFor(element.getModuleInfo())

    fun getOrBuildFirFor(element: KtElement, resolveState: FirModuleResolveState): FirElement =
        resolveState.getOrBuildFirFor(element)

    fun getFirFile(ktFile: KtFile, resolveState: FirModuleResolveState) =
        resolveState.getFirFile(ktFile)

    /**
     * Creates [FirDeclaration] by [KtDeclaration] and runs an [action] with it
     * [ktDeclaration]
     * [FirDeclaration] passed to [action] should not be leaked outside [action] lambda
     * [FirDeclaration] passed to [action] will be resolved at least to [phase]
     * Otherwise, some threading problems may arise,
     *
     * [ktDeclaration] should be non-local declaration (should have fully qualified name)
     */
    inline fun <R> withFirDeclaration(
        ktDeclaration: KtDeclaration,
        resolveState: FirModuleResolveState,
        phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
        action: (FirDeclaration) -> R
    ): R {
        val firDeclaration = resolveState.findSourceFirDeclaration(ktDeclaration)
        resolvedFirToPhase(firDeclaration, phase, resolveState)
        return action(firDeclaration)
    }

    inline fun <reified F : FirDeclaration, R> withFirDeclarationOfType(
        ktDeclaration: KtDeclaration,
        resolveState: FirModuleResolveState,
        action: (F) -> R
    ): R {
        val firDeclaration = resolveState.findSourceFirDeclaration(ktDeclaration)
        if (firDeclaration !is F) throw InvalidFirElementTypeException(ktDeclaration, F::class, firDeclaration::class)
        return action(firDeclaration)
    }

    inline fun <reified F : FirDeclaration, R> withFirDeclarationOfType(
        ktDeclaration: KtLambdaExpression,
        resolveState: FirModuleResolveState,
        action: (F) -> R
    ): R {
        val firDeclaration = resolveState.findSourceFirDeclaration(ktDeclaration)
        if (firDeclaration !is F) throw InvalidFirElementTypeException(ktDeclaration, F::class, firDeclaration::class)
        return action(firDeclaration)
    }

    inline fun <F : FirElement, R> withFir(fir: F, action: (F) -> R): R {
        // TODO locking
        return action(fir)
    }

    fun getDiagnosticsFor(element: KtElement, resolveState: FirModuleResolveState): Collection<Diagnostic> =
        resolveState.getDiagnostics(element)

    fun collectDiagnosticsForFile(ktFile: KtFile, resolveState: FirModuleResolveState): Collection<Diagnostic> =
        resolveState.collectDiagnosticsForFile(ktFile)

    fun <D : FirDeclaration> resolvedFirToPhase(
        firDeclaration: D,
        phase: FirResolvePhase,
        resolveState: FirModuleResolveState
    ): D =
        resolveState.resolvedFirToPhase(firDeclaration, phase)
}



fun KtElement.getOrBuildFir(
    resolveState: FirModuleResolveState,
) = LowLevelFirApiFacade.getOrBuildFirFor(this, resolveState)

inline fun <reified E : FirElement> KtElement.getOrBuildFirSafe(
    resolveState: FirModuleResolveState,
) = LowLevelFirApiFacade.getOrBuildFirFor(this, resolveState) as? E

inline fun <reified E : FirElement> KtElement.getOrBuildFirOfType(
    resolveState: FirModuleResolveState,
): E {
    val fir = LowLevelFirApiFacade.getOrBuildFirFor(this, resolveState)
    if (fir is E) return fir
    throw InvalidFirElementTypeException(this, E::class, fir::class)
}

class InvalidFirElementTypeException(
    ktElement: KtElement,
    expectedFirClass: KClass<out FirElement>,
    actualFirClass: KClass<out FirElement>
) : IllegalStateException("For $ktElement with text `${ktElement.text}` the $expectedFirClass expected, but $actualFirClass found")
