/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase

internal object LLFirSealedClassInheritorsLazyResolver : LLFirLazyResolver(FirResolvePhase.SEALED_CLASS_INHERITORS) {
    override fun createTargetResolver(
        target: LLFirResolveTarget,
    ): LLFirTargetResolver = LLFirSealedClassInheritorsDesignatedResolver(target)

    override fun phaseSpecificCheckIsResolved(target: FirElementWithResolveState) {}
}


/**
 * This resolver is responsible for [SEALED_CLASS_INHERITORS][FirResolvePhase.SEALED_CLASS_INHERITORS] phase.
 *
 * LL FIR uses [LLSealedInheritorsProvider][org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLSealedInheritorsProvider]
 * instead of the compiler [SealedClassInheritorsProviderImpl][org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProviderImpl],
 * so it does nothing during this phase as sealed inheritors will be provided later on demand.
 *
 * @see org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLSealedInheritorsProvider
 * @see org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
 * @see FirResolvePhase.SEALED_CLASS_INHERITORS
 */
private class LLFirSealedClassInheritorsDesignatedResolver(target: LLFirResolveTarget) : LLFirTargetResolver(
    target,
    FirResolvePhase.SEALED_CLASS_INHERITORS,
) {
    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        // just update the phase
    }
}
