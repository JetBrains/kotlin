/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*

private object PartialBodyAnalysisStateKey : FirDeclarationDataKey()

/**
 * Represents the partial (incomplete) body resolve state.
 *
 * If the function body was ever analyzed partially, this attribute must be present.
 * This includes cases when the function was once analyzed partially, and then fully – the attribute still must be there.
 *
 * The attribute must be removed, though, if the declaration phase is reverted (for example, because of in-block modifications).
 *
 * @see LLPartialBodyAnalysisState
 */
internal var FirDeclaration.partialBodyAnalysisState: LLPartialBodyAnalysisState?
        by FirDeclarationDataRegistry.data(PartialBodyAnalysisStateKey)

/**
 * [LLFirResolveTarget] representing single target to resolve. The [target] can be any of [FirElementWithResolveState]
 */
internal class LLFirSingleResolveTarget(designation: FirDesignation) : LLFirResolveTarget(designation) {
    override fun visitTargetElement(
        element: FirElementWithResolveState,
        visitor: LLFirResolveTargetVisitor,
    ) {
        if (element !is FirFile) {
            visitor.performAction(element)
        }
    }
}
