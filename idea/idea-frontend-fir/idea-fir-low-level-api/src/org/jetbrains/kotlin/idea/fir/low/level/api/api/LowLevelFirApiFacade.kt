/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.FirIdeResolveStateService
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.InternalForInline
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSourcesSession
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ktDeclaration
import org.jetbrains.kotlin.idea.util.getElementTextInContext
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
    val firDeclaration = resolveState.findSourceFirDeclaration(this)
    firDeclaration.resolvedFirToPhase(phase, resolveState)
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
 * Executes [action] with given [FirDeclaration]
 * [FirDeclaration] passed to [action] will be resolved at least to [phase] when executing [action] on it
 */
fun <D : FirDeclaration, R> D.withFirDeclaration(
    resolveState: FirModuleResolveState,
    phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
    action: (D) -> R,
): R {
    resolvedFirToPhase(phase, resolveState)
    val originalDeclaration = (this as? FirCallableDeclaration<*>)?.unwrapFakeOverrides() ?: this
    val session = originalDeclaration.session
    return when {
        originalDeclaration.origin == FirDeclarationOrigin.Source
                && session is FirIdeSourcesSession
        -> {
            val cache = session.cache
            val file = resolveState.getFirFile(this, cache)
                ?: error("Fir file was not found for\n${render()}\n${ktDeclaration.getElementTextInContext()}")
            cache.firFileLockProvider.withReadLock(file) { action(this) }
        }
        else -> action(this)
    }
}

/**
 * Returns a list of Diagnostics compiler finds for given [KtElement]
 */
fun KtElement.getDiagnostics(resolveState: FirModuleResolveState): Collection<Diagnostic> =
    resolveState.getDiagnostics(this)

/**
 * Returns a list of Diagnostics compiler finds for given [KtFile]
 */
fun KtFile.collectDiagnosticsForFile(resolveState: FirModuleResolveState): Collection<Diagnostic> =
    resolveState.collectDiagnosticsForFile(this)

/**
 * Resolves a given [FirDeclaration] to [phase] and returns resolved declaration
 *
 * Should not be called form [withFirDeclaration], [withFirDeclarationOfType] functions, as it it may cause deadlock
 */
fun <D : FirDeclaration> D.resolvedFirToPhase(
    phase: FirResolvePhase,
    resolveState: FirModuleResolveState
): D =
    resolveState.resolvedFirToPhase(this, phase)


/**
 * Get a [FirElement] which was created by [KtElement]
 * Returned [FirElement] is guaranteed to be resolved to [FirResolvePhase.BODY_RESOLVE] phase
 */
fun KtElement.getOrBuildFir(
    resolveState: FirModuleResolveState,
): FirElement = resolveState.getOrBuildFirFor(this)

/**
 * Get a [FirElement] which was created by [KtElement], but only if it is subtype of [E], `null` otherwise
 * Returned [FirElement] is guaranteed to be resolved to [FirResolvePhase.BODY_RESOLVE] phase
 */
inline fun <reified E : FirElement> KtElement.getOrBuildFirSafe(
    resolveState: FirModuleResolveState,
) = getOrBuildFir(resolveState) as? E

/**
 * Get a [FirElement] which was created by [KtElement], but only if it is subtype of [E], throws [InvalidFirElementTypeException] otherwise
 * Returned [FirElement] is guaranteed to be resolved to [FirResolvePhase.BODY_RESOLVE] phase
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
fun KtFile.getFirFile(resolveState: FirModuleResolveState): FirFile =
    resolveState.getFirFile(this)

class InvalidFirElementTypeException(
    ktElement: KtElement,
    expectedFirClass: KClass<out FirElement>,
    actualFirClass: KClass<out FirElement>
) : IllegalStateException("For $ktElement with text `${ktElement.text}` the $expectedFirClass expected, but $actualFirClass found")
