/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

internal class LLFirDeclarationTransformer(private val designation: FirDesignation) {
    private val designationWithoutTargetIterator = designation.toSequence(includeTarget = false).iterator()
    private var isInsideTargetDeclaration: Boolean = false
    private var designationPassed: Boolean = false

    inline fun <D> visitDeclarationContent(
        visitor: FirVisitor<Unit, D>,
        declaration: FirDeclaration,
        data: D,
        default: () -> FirDeclaration
    ): FirDeclaration = processDeclarationContent(declaration, default) {
        it.accept(visitor, data)
    }

    inline fun <D> transformDeclarationContent(
        transformer: FirDefaultTransformer<D>,
        declaration: FirDeclaration,
        data: D,
        default: () -> FirDeclaration
    ): FirDeclaration = processDeclarationContent(declaration, default) { toTransform ->
        toTransform.transform<FirElement, D>(transformer, data).also { transformed ->
            check(transformed === toTransform) {
                "become $transformed `${transformed.render()}`, was ${toTransform}: `${toTransform.render()}`"
            }
        }
    }

    private inline fun processDeclarationContent(
        declaration: FirDeclaration,
        default: () -> FirDeclaration,
        applyToDesignated: (FirElementWithResolveState) -> Unit,
    ): FirDeclaration {
        //It means that we are inside the target declaration
        if (isInsideTargetDeclaration) {
            return default()
        }

        //It means that we already transform target declaration and now can skip all others
        if (designationPassed) {
            return declaration
        }

        if (designationWithoutTargetIterator.hasNext()) {
            applyToDesignated(designationWithoutTargetIterator.next())
        } else {
            try {
                isInsideTargetDeclaration = true
                designationPassed = true
                applyToDesignated(designation.target)
            } finally {
                isInsideTargetDeclaration = false
            }
        }

        return declaration
    }

    fun ensureDesignationPassed() {
        check(designationPassed) { "Designation not passed for declaration ${designation.target::class.simpleName}" }
    }
}
