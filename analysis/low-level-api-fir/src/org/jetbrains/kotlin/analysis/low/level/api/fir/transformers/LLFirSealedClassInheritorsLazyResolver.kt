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


private class LLFirSealedClassInheritorsDesignatedResolver(target: LLFirResolveTarget) : LLFirTargetResolver(
    target,
    FirResolvePhase.SEALED_CLASS_INHERITORS,
) {
    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        // just update the phase
    }
}
