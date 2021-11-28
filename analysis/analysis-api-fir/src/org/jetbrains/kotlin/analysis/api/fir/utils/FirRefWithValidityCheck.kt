/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.tokens.assertIsValidAndAccessible
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.withFirDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveType
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase

internal class FirRefWithValidityCheck<out D : FirDeclaration>(
    private val fir: D,
    val resolveState: FirModuleResolveState,
    val token: ValidityToken
) {

    inline fun <R> withFir(phase: FirResolvePhase = FirResolvePhase.RAW_FIR, crossinline action: (fir: D) -> R): R {
        token.assertIsValidAndAccessible()
        return fir.withFirDeclaration(resolveState, phase) { action(it) }
    }

    /**
     * Runs [action] with fir element *without* any lock hold
     * Consider using this only when you are completely sure
     * that fir or one of it's container already holds the lock (i.e, corresponding withFir call was made)
     */
    inline fun <R> withFirUnsafe(action: (fir: D) -> R): R {
        token.assertIsValidAndAccessible()
        return action(fir)
    }

    inline fun <R> withFirAndCache(phase: FirResolvePhase = FirResolvePhase.RAW_FIR, crossinline createValue: (fir: D) -> R) =
        ValidityAwareCachedValue(token) {
            withFir(phase) { fir -> createValue(fir) }
        }

    inline fun <R> withFirByType(type: ResolveType, crossinline action: (fir: D) -> R): R {
        token.assertIsValidAndAccessible()
        return fir.withFirDeclaration(type, resolveState) { action(it) }
    }

    inline fun <R> withFirAndCache(type: ResolveType, crossinline createValue: (fir: D) -> R) =
        ValidityAwareCachedValue(token) {
            withFirByType(type) { fir -> createValue(fir) }
        }

    override fun equals(other: Any?): Boolean {
        if (other !is FirRefWithValidityCheck<*>) return false
        return fir == other.fir && this.token == other.token
    }

    override fun hashCode(): Int {
        return fir.hashCode() * 31 + token.hashCode()
    }

}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <D : FirDeclaration> ValidityTokenOwner.firRef(fir: D, resolveState: FirModuleResolveState) =
    FirRefWithValidityCheck(fir, resolveState, token)
