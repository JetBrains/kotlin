/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.transformers.FirAbstractPhaseTransformer
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationUntypedDesignation

internal class IDEDeclarationTransformer(private val designation: FirDeclarationUntypedDesignation) {
    private val designationWithoutTargetIterator = designation.toSequence(includeTarget = false).iterator()
    private var isInsideTargetDeclaration: Boolean = false
    private var designationPassed: Boolean = false

    inline fun <K : FirDeclaration, D> visitDeclarationContent(
        visitor: FirVisitor<Unit, D>,
        declaration: K,
        data: D,
        default: () -> K
    ) = processDeclarationContent(declaration, default) {
        it.accept(visitor, data)
    }

    inline fun <K : FirDeclaration, D> transformDeclarationContent(
        transformer: FirDefaultTransformer<D>,
        declaration: K,
        data: D,
        default: () -> K
    ): K = processDeclarationContent(declaration, default) { toTransform ->
        toTransform.transform<FirElement, D>(transformer, data).also { transformed ->
            check(transformed === toTransform) {
                "become $transformed `${transformed.render()}`, was ${toTransform}: `${toTransform.render()}`"
            }
        }
    }

    inline fun <K : FirDeclaration> processDeclarationContent(
        declaration: K,
        default: () -> K,
        applyToDesignated: (FirDeclaration) -> Unit,
    ): K {
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
                applyToDesignated(designation.declaration)
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