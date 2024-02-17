/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirCompanionGenerationTransformer

internal object LLFirGeneratedCompanionObjectLazyResolver : LLFirLazyResolver(FirResolvePhase.COMPANION_GENERATION) {
    override fun createTargetResolver(target: LLFirResolveTarget): LLFirTargetResolver = LLFirCompanionGenerationTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: FirElementWithResolveState) {}
}

private class LLFirCompanionGenerationTargetResolver(target: LLFirResolveTarget) : LLFirTargetResolver(
    target,
    FirResolvePhase.COMPANION_GENERATION,
) {
    private val transformer: FirCompanionGenerationTransformer = FirCompanionGenerationTransformer(resolveTargetSession)

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        if (target !is FirRegularClass) return
        transformer.generateAndUpdateCompanion(target)
    }
}
