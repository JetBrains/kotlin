/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.FirIdeResolveStateService
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.InternalForInline
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.ResolveType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import kotlin.reflect.KClass

/**
 * Returns [FirModuleResolveState] which corresponds to containing module
 */
fun KtElement.getResolveState(): FirModuleResolveState =
    getModuleInfo().getResolveState()

/**
 * Returns [FirModuleResolveState] which corresponds to containing module
 */
fun IdeaModuleInfo.getResolveState(): FirModuleResolveState =
    FirIdeResolveStateService.getInstance(project!!).getResolveState(this)


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
    val firDeclaration = if (containingKtFile.isCompiled) {
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
    firDeclaration.resolvedFirToType(resolveType, resolveState)
    return action(firDeclaration)
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
    if (firDeclaration !is F) throw InvalidFirElementTypeException(this, F::class, firDeclaration::class)
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
    if (firDeclaration !is F) throw InvalidFirElementTypeException(this, F::class, firDeclaration::class)
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
    resolvedFirToPhase(phase, resolveState)
    return resolveState.withLock(this, DeclarationLockType.READ_LOCK, action)
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
    resolvedFirToType(type, resolveState)
    return resolveState.withLock(this, DeclarationLockType.READ_LOCK, action)
}

/**
 * Executes [action] with given [FirDeclaration] under write lock, so resolve **is possible** inside [action]
 */
fun <D : FirDeclaration, R> D.withFirDeclarationInWriteLock(
    resolveState: FirModuleResolveState,
    phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
    action: (D) -> R,
): R {
    resolvedFirToPhase(phase, resolveState)
    return resolveState.withLock(this, DeclarationLockType.WRITE_LOCK, action)
}

/**
 * Executes [action] with given [FirDeclaration] under write lock, so resolve **is possible** inside [action]
 */
fun <D : FirDeclaration, R> D.withFirDeclarationInWriteLock(
    resolveState: FirModuleResolveState,
    resolveType: ResolveType = ResolveType.BodyResolveWithChildren,
    action: (D) -> R,
): R {
    resolvedFirToType(resolveType, resolveState)
    return resolveState.withLock(this, DeclarationLockType.WRITE_LOCK, action)
}

/**
 * Returns a list of Diagnostics compiler finds for given [KtElement]
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
fun KtElement.getDiagnostics(resolveState: FirModuleResolveState, filter: DiagnosticCheckerFilter): Collection<FirPsiDiagnostic<*>> =
    resolveState.getDiagnostics(this, filter)

/**
 * Returns a list of Diagnostics compiler finds for given [KtFile]
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
fun KtFile.collectDiagnosticsForFile(
    resolveState: FirModuleResolveState,
    filter: DiagnosticCheckerFilter
): Collection<FirPsiDiagnostic<*>> =
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
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
fun KtElement.getOrBuildFir(
    resolveState: FirModuleResolveState,
): FirElement = resolveState.getOrBuildFirFor(this)

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
    throw InvalidFirElementTypeException(this, E::class, fir::class)
}

/**
 * Get a [FirFile] which was created by [KtElement]
 * Returned [FirFile] can be resolved to any phase from [FirResolvePhase.RAW_FIR] to [FirResolvePhase.BODY_RESOLVE]
 */
fun KtFile.getOrBuildFirFile(resolveState: FirModuleResolveState): FirFile =
    resolveState.getOrBuildFirFile(this)

class InvalidFirElementTypeException(
    ktElement: KtElement,
    expectedFirClass: KClass<out FirElement>,
    actualFirClass: KClass<out FirElement>
) : IllegalStateException("For $ktElement with text `${ktElement.text}` the $expectedFirClass expected, but $actualFirClass found")
