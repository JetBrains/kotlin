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

/**
 * Resolves a single callable declaration.
 * If available, performs partial body analysis.
 */
internal class LLFirPartialBodyResolveTarget(
    designation: FirDesignation,
    val request: LLPartialBodyResolveRequest
) : LLFirResolveTarget(designation) {
    override fun visitTargetElement(element: FirElementWithResolveState, visitor: LLFirResolveTargetVisitor) {
        visitor.performAction(element)
    }
}

/**
 * A partial body analysis request.
 *
 * @param target A callable to be analyzed. The [target] is required to be partial body resolvable (see [isPartialBodyResolvable]).
 * @param totalPsiStatementCount The total number of statements in the AST.
 * @param targetPsiStatementCount The number of statements in the AST to be analyzed as a result of this request.
 * @param stopElement The first element that does not belong to the analyzed part of the declaration. If `null`, the whole [target] body
 *        is analyzed.
 */
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