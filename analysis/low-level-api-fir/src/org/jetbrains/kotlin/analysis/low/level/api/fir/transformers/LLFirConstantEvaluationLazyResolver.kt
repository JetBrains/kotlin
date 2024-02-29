/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase

internal object LLFirConstantEvaluationLazyResolver : LLFirLazyResolver(FirResolvePhase.CONSTANT_EVALUATION) {
    override fun createTargetResolver(target: LLFirResolveTarget): LLFirTargetResolver = LLFirConstantEvaluationTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: FirElementWithResolveState) {}
}

/**
 * This resolver is responsible for [CONSTANT_EVALUATION][FirResolvePhase.CONSTANT_EVALUATION] phase.
 *
 * This resolver doesn't do anything yet (see KT-64151).
 *
 * @see FirResolvePhase.CONSTANT_EVALUATION
 */
private class LLFirConstantEvaluationTargetResolver(resolveTarget: LLFirResolveTarget) : LLFirTargetResolver(
    resolveTarget,
    FirResolvePhase.CONSTANT_EVALUATION,
) {
    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        // Will be finished later on
    }
}
