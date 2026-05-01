/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase

@KaImplementationDetail
interface LLEvent {
    fun notifyCompleted()
    fun notifyCompletedWithFailure(throwable: Throwable)
}

@KaImplementationDetail
internal interface LLEventTracker : FirSessionComponent {
    fun phase(
        target: FirElementWithResolveState,
        containingDeclarations: List<FirDeclaration>,
        requestedPhase: FirResolvePhase,
    ): LLEvent?
}
