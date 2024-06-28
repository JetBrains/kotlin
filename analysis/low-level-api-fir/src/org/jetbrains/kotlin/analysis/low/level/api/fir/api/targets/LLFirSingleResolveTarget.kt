/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*

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
