/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.trasformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.PrivateForInline

internal class IDEDeclarationTransformer(private val designation: FirDesignation) {
    private var isInsideTargetDeclaration: Boolean = false

    private inline fun <R> insideTargetDeclaration(insideCurrent: Boolean, action: () -> R): R {
        val oldValue = isInsideTargetDeclaration
        isInsideTargetDeclaration = insideCurrent
        try {
            return action()
        } finally {
            isInsideTargetDeclaration = oldValue
        }
    }

    inline fun transformDeclarationContent(
        transformer: FirAbstractBodyResolveTransformer,
        declaration: FirDeclaration,
        data: ResolutionMode,
        transformDeclaration: (FirDeclaration, ResolutionMode) -> CompositeTransformResult<FirDeclaration>
    ): CompositeTransformResult<FirDeclaration> {
        return if (designation.canGoNext()) {
            val declarationToTransform = designation.currentDeclaration
            val isTargetDeclaration = designation.isTargetDeclaration()
            designation.goNext()
            insideTargetDeclaration(isTargetDeclaration) {
                declarationToTransform.visitNoTransform(transformer, data)
            }
            declaration.compose()
        } else {
            if (isInsideTargetDeclaration) {
                transformDeclaration(declaration, data)
            } else {
                declaration.compose()
            }
        }
    }

    fun needReplacePhase(firDeclaration: FirDeclaration): Boolean =
        isInsideTargetDeclaration
}

private fun <D> FirElement.visitNoTransform(transformer: FirTransformer<D>, data: D) {
    val result = this.transform<FirElement, D>(transformer, data)
    require(result.single === this) { "become ${result.single}: `${result.single.render()}`, was ${this}: `${this.render()}`" }
}
