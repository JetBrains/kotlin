/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.impl.barebone.annotations.InternalForInline
import org.jetbrains.kotlin.analysis.low.level.api.fir.FirIdeResolveStateService
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveType
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression

/**
 * Returns [FirModuleResolveState] which corresponds to containing module
 */
fun KtElement.getResolveState(): FirModuleResolveState {
    val project = project
    return getKtModule(project).getResolveState(project)
}

/**
 * Returns [FirModuleResolveState] which corresponds to containing module
 */
fun KtModule.getResolveState(project: Project): FirModuleResolveState =
    FirIdeResolveStateService.getInstance(project).getResolveState(this)


/**
 * Creates [FirDeclaration] by [KtDeclaration] and executes an [action] on it
 * [FirDeclaration] passed to [action] will be resolved at least to [phase] when executing [action] on it
 *
 * [FirDeclaration] passed to [action] should not be leaked outside [action] lambda
 * Otherwise, some threading problems may arise,
 */
@OptIn(InternalForInline::class)
inline fun <R> KtDeclaration.withFirDeclaration(
    resolveState: FirModuleResolveState,
    phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
    action: (FirDeclaration) -> R
): R {
    val firDeclaration = if (getKtModule(project) !is KtSourceModule) {
        resolveState.findSourceFirCompiledDeclaration(this)
    } else {
        resolveState.findSourceFirDeclaration(this)
    }

    val resolvedDeclaration = if (firDeclaration.resolvePhase < phase) {
        firDeclaration.resolvedFirToPhase(phase, resolveState)
    } else {
        firDeclaration
    }

    return action(resolvedDeclaration)
}

/**
 * Creates [FirDeclaration] by [KtDeclaration] and executes an [action] on it
 * [FirDeclaration] passed to [action] will be resolved at least to [resolveType] when executing [action] on it
 *
 * [FirDeclaration] passed to [action] should not be leaked outside [action] lambda
 * Otherwise, some threading problems may arise,
 */
@OptIn(InternalForInline::class)
inline fun <R> KtDeclaration.withFirDeclaration(
    resolveState: FirModuleResolveState,
    resolveType: ResolveType = ResolveType.NoResolve,
    action: (FirDeclaration) -> R
): R {
    val firDeclaration = resolveState.findSourceFirDeclaration(this)
    val resolvedDeclaration = firDeclaration.resolvedFirToType(resolveType, resolveState)
    return action(resolvedDeclaration)
}

/**
 * Creates [FirDeclaration] by [KtDeclaration] and executes an [action] on it
 * [FirDeclaration] passed to [action] will be resolved at least to [phase] when executing [action] on it
 *
 * If resulted [FirDeclaration] is not [F] throws [InvalidFirElementTypeException]
 *
 * [FirDeclaration] passed to [action] should not be leaked outside [action] lambda
 * Otherwise, some threading problems may arise,
 */
@OptIn(InternalForInline::class)
inline fun <reified F : FirDeclaration, R> KtDeclaration.withFirDeclarationOfType(
    resolveState: FirModuleResolveState,
    phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
    action: (F) -> R
): R = withFirDeclaration(resolveState, phase) { firDeclaration ->
    if (firDeclaration !is F) throwUnexpectedFirElementError(firDeclaration, this, F::class)
    action(firDeclaration)
}

/**
 * Creates [FirDeclaration] by [KtLambdaExpression] and executes an [action] on it
 *
 * If resulted [FirDeclaration] is not [F] throws [InvalidFirElementTypeException]
 *
 * [FirDeclaration] passed to [action] should not be leaked outside [action] lambda
 * Otherwise, some threading problems may arise,
 */
@OptIn(InternalForInline::class)
inline fun <reified F : FirDeclaration, R> KtLambdaExpression.withFirDeclarationOfType(
    resolveState: FirModuleResolveState,
    action: (F) -> R
): R {
    val firDeclaration = resolveState.findSourceFirDeclaration(this)
    if (firDeclaration !is F) throwUnexpectedFirElementError(firDeclaration, this, F::class)
    return action(firDeclaration)
}

/**
 * Executes [action] with given [FirDeclaration] under read action, so resolve **is not possible** inside [action]
 * [FirDeclaration] passed to [action] will be resolved at least to [phase] when executing [action] on it
 */
fun <D : FirDeclaration, R> D.withFirDeclaration(
    resolveState: FirModuleResolveState,
    phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
    action: (D) -> R,
): R {
    val resolvedDeclaration = resolvedFirToPhase(phase, resolveState)
    return action(resolvedDeclaration)
}

/**
 * Executes [action] with given [FirDeclaration] under read action, so resolve **is not possible** inside [action]
 * [FirDeclaration] passed to [action] will be resolved at least to [phase] when executing [action] on it
 */
fun <D : FirDeclaration, R> D.withFirDeclaration(
    type: ResolveType,
    resolveState: FirModuleResolveState,
    action: (D) -> R,
): R {
    val resolvedDeclaration = resolvedFirToType(type, resolveState)
    return action(resolvedDeclaration)
}

/**
 * Returns a list of Diagnostics compiler finds for given [KtElement]
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
fun KtElement.getDiagnostics(resolveState: FirModuleResolveState, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic> =
    resolveState.getDiagnostics(this, filter)

/**
 * Returns a list of Diagnostics compiler finds for given [KtFile]
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
fun KtFile.collectDiagnosticsForFile(
    resolveState: FirModuleResolveState,
    filter: DiagnosticCheckerFilter
): Collection<KtPsiDiagnostic> =
    resolveState.collectDiagnosticsForFile(this, filter)

/**
 * Resolves a given [FirDeclaration] to [phase] and returns resolved declaration
 *
 * Should not be called form [withFirDeclaration], [withFirDeclarationOfType] functions, as it it may cause deadlock
 */
fun <D : FirDeclaration> D.resolvedFirToPhase(
    phase: FirResolvePhase,
    resolveState: FirModuleResolveState
): D =
    resolveState.resolveFirToPhase(this, phase)

/**
 * Resolves a given [FirDeclaration] to [phase] and returns resolved declaration
 *
 * Should not be called form [withFirDeclaration], [withFirDeclarationOfType] functions, as it it may cause deadlock
 */
fun <D : FirDeclaration> D.resolvedFirToType(
    type: ResolveType,
    resolveState: FirModuleResolveState
): D =
    resolveState.resolveFirToResolveType(this, type)


/**
 * Get a [FirElement] which was created by [KtElement]
 * Returned [FirElement] is guaranteed to be resolved to [FirResolvePhase.BODY_RESOLVE] phase
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase.
 *
 * The `null` value is returned iff FIR tree does not have corresponding element
 */
fun KtElement.getOrBuildFir(
    resolveState: FirModuleResolveState,
): FirElement? = resolveState.getOrBuildFirFor(this)

/**
 * Get a [FirElement] which was created by [KtElement], but only if it is subtype of [E], `null` otherwise
 * Returned [FirElement] is guaranteed to be resolved to [FirResolvePhase.BODY_RESOLVE] phase
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
inline fun <reified E : FirElement> KtElement.getOrBuildFirSafe(
    resolveState: FirModuleResolveState,
) = getOrBuildFir(resolveState) as? E

/**
 * Get a [FirElement] which was created by [KtElement], but only if it is subtype of [E], throws [InvalidFirElementTypeException] otherwise
 * Returned [FirElement] is guaranteed to be resolved to [FirResolvePhase.BODY_RESOLVE] phase
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
inline fun <reified E : FirElement> KtElement.getOrBuildFirOfType(
    resolveState: FirModuleResolveState,
): E {
    val fir = this.getOrBuildFir(resolveState)
    if (fir is E) return fir
    throwUnexpectedFirElementError(fir, this, E::class)
}

/**
 * Get a [FirFile] which was created by [KtElement]
 * Returned [FirFile] can be resolved to any phase from [FirResolvePhase.RAW_FIR] to [FirResolvePhase.BODY_RESOLVE]
 */
fun KtFile.getOrBuildFirFile(resolveState: FirModuleResolveState): FirFile =
    resolveState.getOrBuildFirFile(this)
