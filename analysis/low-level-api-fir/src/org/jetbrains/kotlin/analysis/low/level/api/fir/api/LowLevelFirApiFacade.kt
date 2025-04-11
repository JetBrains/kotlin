/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLResolutionFacadeService
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

/**
 * Returns [LLResolutionFacade] which corresponds to containing module
 */
fun KaModule.getResolutionFacade(project: Project): LLResolutionFacade =
    LLResolutionFacadeService.getInstance(project).getResolutionFacade(this)


/**
 * Creates [FirBasedSymbol] by [KtDeclaration] .
 * returned [FirDeclaration]  will be resolved at least to [phase]
 *
 */
fun KtDeclaration.resolveToFirSymbol(
    resolutionFacade: LLResolutionFacade,
    phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
): FirBasedSymbol<*> {
    return resolutionFacade.resolveToFirSymbol(this, phase)
}

/**
 * Creates [FirBasedSymbol] by [KtDeclaration] .
 * returned [FirDeclaration] will be resolved at least to [phase]
 *
 * If resulted [FirBasedSymbol] is not subtype of [S], throws [InvalidFirElementTypeException]
 */
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
inline fun <reified S : FirBasedSymbol<*>> KtDeclaration.resolveToFirSymbolOfType(
    resolutionFacade: LLResolutionFacade,
    phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
): @kotlin.internal.NoInfer S {
    val symbol = resolveToFirSymbol(resolutionFacade, phase)
    if (symbol !is S) {
        throwUnexpectedFirElementError(symbol, this, S::class)
    }
    return symbol
}

/**
 * Creates [FirBasedSymbol] by [KtDeclaration] .
 * returned [FirDeclaration] will be resolved at least to [phase]
 *
 * If resulted [FirBasedSymbol] is not subtype of [S], returns `null`
 */
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
inline fun <reified S : FirBasedSymbol<*>> KtDeclaration.resolveToFirSymbolOfTypeSafe(
    resolutionFacade: LLResolutionFacade,
    phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
): @kotlin.internal.NoInfer S? {
    return resolveToFirSymbol(resolutionFacade, phase) as? S
}


/**
 * Returns a list of Diagnostics compiler finds for given [KtElement]
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
fun KtElement.getDiagnostics(resolutionFacade: LLResolutionFacade, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic> =
    resolutionFacade.getDiagnostics(this, filter)

/**
 * Returns a list of Diagnostics compiler finds for given [KtFile]
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
fun KtFile.collectDiagnosticsForFile(
    resolutionFacade: LLResolutionFacade,
    filter: DiagnosticCheckerFilter
): Collection<KtPsiDiagnostic> =
    resolutionFacade.collectDiagnosticsForFile(this, filter)

/**
 * Build [FirElement] node in its final resolved state for a requested element.
 *
 * Note: that it isn't always [BODY_RESOLVE][FirResolvePhase.BODY_RESOLVE]
 * as not all declarations have types/bodies/etc. to resolve.
 *
 * This operation could be time-consuming because it creates
 * [FileStructureElement][org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructureElement]
 * and may resolve non-local declarations into [BODY_RESOLVE][FirResolvePhase.BODY_RESOLVE] phase.
 *
 * Please use [getOrBuildFirFile] to get [FirFile] in undefined phase.
 *
 * @return associated [FirElement] in final resolved state if it exists.
 *
 * @see getOrBuildFirFile
 * @see LLResolutionFacade.getOrBuildFirFor
 */
fun KtElement.getOrBuildFir(resolutionFacade: LLResolutionFacade): FirElement? =
    resolutionFacade.getOrBuildFirFor(this)

/**
 * Get a [FirElement] which was created by [KtElement], but only if it is subtype of [E], `null` otherwise
 * Returned [FirElement] is guaranteed to be resolved to [FirResolvePhase.BODY_RESOLVE] phase
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
inline fun <reified E : FirElement> KtElement.getOrBuildFirSafe(resolutionFacade: LLResolutionFacade) =
    getOrBuildFir(resolutionFacade) as? E

/**
 * Get a [FirElement] which was created by [KtElement], but only if it is subtype of [E], throws [InvalidFirElementTypeException] otherwise
 * Returned [FirElement] is guaranteed to be resolved to [FirResolvePhase.BODY_RESOLVE] phase
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
inline fun <reified E : FirElement> KtElement.getOrBuildFirOfType(resolutionFacade: LLResolutionFacade): E {
    val fir = getOrBuildFir(resolutionFacade)
    if (fir is E) return fir
    throwUnexpectedFirElementError(fir, this, E::class)
}

/**
 * Get a [FirFile] which was created by [KtElement]
 * Returned [FirFile] can be resolved to any phase from [FirResolvePhase.RAW_FIR] to [FirResolvePhase.BODY_RESOLVE]
 */
fun KtFile.getOrBuildFirFile(resolutionFacade: LLResolutionFacade): FirFile =
    resolutionFacade.getOrBuildFirFile(this)
