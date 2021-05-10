/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.transformers.FirAbstractPhaseTransformer
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationUntypedDesignation

internal class IDEDeclarationTransformer(private val designation: FirDeclarationUntypedDesignation) {
    private val designationWithoutTargetIterator = designation.toSequence(includeTarget = false).iterator()
    private var isInsideTargetDeclaration: Boolean = false
    private var designationPassed: Boolean = false

    inline fun <K, D> transformDeclarationContent(
        transformer: FirAbstractPhaseTransformer<D>,
        declaration: K,
        data: D,
        defaultCallTransform: () -> K
    ): K {
        //It means that we are inside the target declaration
        if (isInsideTargetDeclaration) {
            return defaultCallTransform()
        }

        //It means that we already transform target declaration and now can skip all others
        if (designationPassed) {
            return declaration
        }

        if (designationWithoutTargetIterator.hasNext()) {
            designationWithoutTargetIterator.next().visitNoTransform(transformer, data)
        } else {
            try {
                isInsideTargetDeclaration = true
                designationPassed = true
                designation.declaration.visitNoTransform(transformer, data)
            } finally {
                isInsideTargetDeclaration = false
            }
        }

        return declaration
    }

    val needReplacePhase: Boolean get() = isInsideTargetDeclaration

    fun ensureDesignationPassed() {
        check(designationPassed) { "Designation not passed for declaration ${designation.declaration::class.simpleName}" }
    }
}

private fun <D> FirElement.visitNoTransform(transformer: FirTransformer<D>, data: D) {
    val result = this.transform<FirElement, D>(transformer, data)
    require(result === this) { "become $result `${result.render()}`, was ${this}: `${this.render()}`" }
}
