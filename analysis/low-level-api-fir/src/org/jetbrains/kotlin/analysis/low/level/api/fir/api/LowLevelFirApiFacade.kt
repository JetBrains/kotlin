/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirResolveSessionService
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

/**
 * Returns [LLFirResolveSession] which corresponds to containing module
 */
fun KtModule.getFirResolveSession(project: Project): LLFirResolveSession =
    LLFirResolveSessionService.getInstance(project).getFirResolveSession(this)


/**
 * Creates [FirBasedSymbol] by [KtDeclaration] .
 * returned [FirDeclaration]  will be resolved at least to [phase]
 *
 */
fun KtDeclaration.resolveToFirSymbol(
    firResolveSession: LLFirResolveSession,
    phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
): FirBasedSymbol<*> {
    return firResolveSession.resolveToFirSymbol(this, phase)
}

/**
 * Creates [FirBasedSymbol] by [KtDeclaration] .
 * returned [FirDeclaration] will be resolved at least to [phase]
 *
 * If resulted [FirBasedSymbol] is not subtype of [S], throws [InvalidFirElementTypeException]
 */
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
inline fun <reified S : FirBasedSymbol<*>> KtDeclaration.resolveToFirSymbolOfType(
    firResolveSession: LLFirResolveSession,
    phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
): @kotlin.internal.NoInfer S {
    val symbol = resolveToFirSymbol(firResolveSession, phase)
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
    firResolveSession: LLFirResolveSession,
    phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
): @kotlin.internal.NoInfer S? {
    return resolveToFirSymbol(firResolveSession, phase) as? S
}


/**
 * Returns a list of Diagnostics compiler finds for given [KtElement]
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
fun KtElement.getDiagnostics(firResolveSession: LLFirResolveSession, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic> =
    firResolveSession.getDiagnostics(this, filter)

/**
 * Returns a list of Diagnostics compiler finds for given [KtFile]
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
fun KtFile.collectDiagnosticsForFile(
    firResolveSession: LLFirResolveSession,
    filter: DiagnosticCheckerFilter
): Collection<KtPsiDiagnostic> =
    firResolveSession.collectDiagnosticsForFile(this, filter)

/**
 * Get a [FirElement] which was created by [KtElement]
 * Returned [FirElement] is guaranteed to be resolved to [FirResolvePhase.BODY_RESOLVE] phase
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase.
 *
 * The `null` value is returned iff FIR tree does not have corresponding element
 */
fun KtElement.getOrBuildFir(
    firResolveSession: LLFirResolveSession,
): FirElement? = firResolveSession.getOrBuildFirFor(this)

/**
 * Get a [FirElement] which was created by [KtElement], but only if it is subtype of [E], `null` otherwise
 * Returned [FirElement] is guaranteed to be resolved to [FirResolvePhase.BODY_RESOLVE] phase
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
inline fun <reified E : FirElement> KtElement.getOrBuildFirSafe(
    firResolveSession: LLFirResolveSession,
) = getOrBuildFir(firResolveSession) as? E

/**
 * Get a [FirElement] which was created by [KtElement], but only if it is subtype of [E], throws [InvalidFirElementTypeException] otherwise
 * Returned [FirElement] is guaranteed to be resolved to [FirResolvePhase.BODY_RESOLVE] phase
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
inline fun <reified E : FirElement> KtElement.getOrBuildFirOfType(
    firResolveSession: LLFirResolveSession,
): E {
    val fir = getOrBuildFir(firResolveSession)
    if (fir is E) return fir
    throwUnexpectedFirElementError(fir, this, E::class)
}

/**
 * Get a [FirFile] which was created by [KtElement]
 * Returned [FirFile] can be resolved to any phase from [FirResolvePhase.RAW_FIR] to [FirResolvePhase.BODY_RESOLVE]
 */
fun KtFile.getOrBuildFirFile(firResolveSession: LLFirResolveSession): FirFile =
    firResolveSession.getOrBuildFirFile(this)
