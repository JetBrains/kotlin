/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.utils

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.withFirDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.api.withFirDeclarationInWriteLock
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.tokens.assertIsValidAndAccessible
import java.lang.ref.WeakReference

internal class FirRefWithValidityCheck<out D : FirDeclaration>(fir: D, resolveState: FirModuleResolveState, val token: ValidityToken) {
    private val firWeakRef = WeakReference(fir)
    private val resolveStateWeakRef = WeakReference(resolveState)

    @TestOnly
    internal fun isCollected(): Boolean =
        firWeakRef.get() == null && resolveStateWeakRef.get() == null

    inline fun <R> withFir(phase: FirResolvePhase = FirResolvePhase.RAW_FIR, crossinline action: (fir: D) -> R): R {
        token.assertIsValidAndAccessible()
        val fir = firWeakRef.get()
            ?: throw EntityWasGarbageCollectedException("FirElement")
        val resolveState = resolveStateWeakRef.get()
            ?: throw EntityWasGarbageCollectedException("FirModuleResolveState")
        return when (phase) {
            FirResolvePhase.BODY_RESOLVE -> {
                /*
                 The BODY_RESOLVE phase is the maximum possible phase we can resolve our declaration to
                 So there is not need to run whole `action` under read lock
                 */
                action(fir.withFirDeclaration(resolveState, phase) { it })
            }
            else -> fir.withFirDeclaration(resolveState, phase) { action(it) }
        }
    }

    /**
     * Runs [action] with fir element *without* any lock hold
     * Consider using this only when you are completely sure
     * that fir or one of it's container already holds the lock (i.e, corresponding withFir call was made)
     */
    inline fun <R> withFirUnsafe(action: (fir: D) -> R): R {
        token.assertIsValidAndAccessible()
        val fir = firWeakRef.get()
            ?: throw EntityWasGarbageCollectedException("FirElement")
        return action(fir)
    }

    /**
     * Runs [action] with fir element with write action hold
     * Consider using this then [action] may call some resolve
     */
    inline fun <R> withFirWithPossibleResolveInside(
        phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
        crossinline action: (fir: D) -> R
    ): R {
        token.assertIsValidAndAccessible()
        val fir = firWeakRef.get()
            ?: throw EntityWasGarbageCollectedException("FirElement")
        val resolveState = resolveStateWeakRef.get()
            ?: throw EntityWasGarbageCollectedException("FirModuleResolveState")
        return when (phase) {
            FirResolvePhase.BODY_RESOLVE -> {
                /*
                 The BODY_RESOLVE phase is the maximum possible phase we can resolve our declaration to
                 So there is not need to run whole `action` under write lock
                 */
                action(fir.withFirDeclarationInWriteLock(resolveState, phase) { it })
            }
            else -> fir.withFirDeclarationInWriteLock(resolveState, phase) { action(it) }
        }
    }

    val resolveState
        get() = resolveStateWeakRef.get() ?: throw EntityWasGarbageCollectedException("FirModuleResolveState")

    inline fun <R> withFirAndCache(phase: FirResolvePhase = FirResolvePhase.RAW_FIR, crossinline createValue: (fir: D) -> R) =
        ValidityAwareCachedValue(token) {
            withFir(phase) { fir -> createValue(fir) }
        }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <D : FirDeclaration> ValidityTokenOwner.firRef(fir: D, resolveState: FirModuleResolveState) =
    FirRefWithValidityCheck(fir, resolveState, token)