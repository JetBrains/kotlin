/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.PrivateForInline
import java.lang.ref.WeakReference

class WeakFirRef<D : FirDeclaration>(fir: D, resolveState: FirModuleResolveState) {
    @PrivateForInline
    val firWeakRef = WeakReference(fir)

    @PrivateForInline
    val resolveStateWeakRef = WeakReference(resolveState)

    @OptIn(PrivateForInline::class)
    inline fun <R> withFir(phase: FirResolvePhase = FirResolvePhase.RAW_FIR, action: (fir: D) -> R): R {
        val fir = firWeakRef.get()
            ?: error("FirElement was garbage collected")
        val resolveState = resolveStateWeakRef.get()
            ?: error("FirModuleResolveState was garbage collected")
        return action(LowLevelFirApiFacade.resolvedFirToPhase(fir, phase, resolveState))
    }
}