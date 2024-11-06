/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isPartialBodyResolvable
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.psi.KtElement

internal class LLFirPartialBodyResolveTarget(
    designation: FirDesignation,
    val request: LLPartialBodyResolveRequest
) : LLFirResolveTarget(designation) {
    override fun visitTargetElement(element: FirElementWithResolveState, visitor: LLFirResolveTargetVisitor) {
        visitor.performAction(element)
    }
}

internal class LLPartialBodyResolveRequest(
    val target: FirDeclaration,
    val totalPsiStatementCount: Int,
    val targetPsiStatementCount: Int,
    val stopElement: KtElement?
) {
    init {
        require(target.isPartialBodyResolvable)
    }
}