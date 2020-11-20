/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.PrivateForInline
import org.jetbrains.kotlin.psi.KtDeclaration
import java.lang.ref.WeakReference
import kotlin.reflect.KClass


/**
 * Lazy [FirDeclaration] reference which allows to find corresponding non-local [FirDeclaration] by [KtDeclaration]
 * Builds declaration only when [withFir] is called or some other entity already built it
 * Allows approximately to check if we already have corresponding [FirDeclaration] built by [isFirDeclarationAlreadyBuilt]
 *
 * To create one consider using [weakByPsiRef]
 */
class WeakFirByPsiRef<KT : KtDeclaration, FIR : FirDeclaration> @PrivateForInline constructor(
    val ktDeclaration: KT,
    @PrivateForInline val firClass: KClass<FIR>,
    resolveState: FirModuleResolveState,
) {
    @PrivateForInline
    val resolveStateWeakRef = WeakReference(resolveState)

    /**
     * Checks if corresponding [FirDeclaration] is available now
     * If return true consequent call to [withFir] will not build new RAW_FIR as it is already built
     * If return false consequent call to [withFir] will build new [FirDeclaration]
     * if it was not build between [isFirDeclarationAlreadyBuilt] & [withFir] calls
     *
     * Should be used with caution as this is only approximation and may return inaccurate results
     */
    @OptIn(PrivateForInline::class)
    fun isFirDeclarationAlreadyBuilt(): Boolean {
        val resolveState = resolveStateWeakRef.get()
            ?: error("FirModuleResolveState was garbage collected")
        return resolveState.isFirFileBuilt(ktDeclaration.containingKtFile)
    }

    /**
     * Creates [FirDeclaration] by [KtDeclaration] if it is not created previously and runs an [action] with it
     * [FirDeclaration] passed to [action] should not be leaked outside [action] lambda
     * Otherwise, some threading problems may arise.`
     *
     * [FirDeclaration] passed to [action] will be resolved at least to [phase]
     **/
    @OptIn(PrivateForInline::class)
    inline fun <R> withFir(phase: FirResolvePhase = FirResolvePhase.RAW_FIR, action: (fir: FIR) -> R): R {
        val resolveState = resolveStateWeakRef.get()
            ?: error("FirModuleResolveState was garbage collected")
        return LowLevelFirApiFacade.withFirDeclaration(ktDeclaration, resolveState, phase) { fir ->
            if (!firClass.isInstance(fir)) throw InvalidFirElementTypeException(ktDeclaration, firClass, fir::class)
            @Suppress("UNCHECKED_CAST")
            action(fir as FIR)
        }
    }
}

@OptIn(PrivateForInline::class)
inline fun <KT : KtDeclaration, reified FIR : FirDeclaration> weakByPsiRef(
    ktDeclaration: KT,
    resolveState: FirModuleResolveState,
) = WeakFirByPsiRef(ktDeclaration, FIR::class, resolveState)