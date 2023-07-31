/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.util.forEachDependentDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isScriptDependentDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isScriptStatement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.visitors.FirVisitor

internal object LLFirPhaseUpdater {
    fun updateDeclarationInternalsPhase(
        target: FirElementWithResolveState,
        newPhase: FirResolvePhase,
        updateForLocalDeclarations: Boolean,
    ) {
        updatePhaseForNonLocals(target, newPhase, isTargetDeclaration = true)

        if (updateForLocalDeclarations) {
            when (target) {
                is FirFunction -> target.body?.accept(PhaseUpdatingTransformer, newPhase)
                is FirVariable -> {
                    target.initializer?.accept(PhaseUpdatingTransformer, newPhase)
                    target.delegate?.accept(PhaseUpdatingTransformer, newPhase)
                    target.getter?.body?.accept(PhaseUpdatingTransformer, newPhase)
                    target.setter?.body?.accept(PhaseUpdatingTransformer, newPhase)
                    target.backingField?.accept(PhaseUpdatingTransformer, newPhase)
                }

                is FirScript -> {
                    target.parameters.forEach { it.accept(PhaseUpdatingTransformer, newPhase) }
                    for (statement in target.statements) {
                        if (!statement.isScriptStatement) continue
                        statement.accept(PhaseUpdatingTransformer, newPhase)
                    }
                }
            }
        }
    }


    private fun updatePhaseForNonLocals(element: FirElementWithResolveState, newPhase: FirResolvePhase, isTargetDeclaration: Boolean) {
        if (element.resolvePhase >= newPhase) return
        if (!isTargetDeclaration) {
            // phase update for target declaration happens as a declaration publication event after resolve is finished
            @OptIn(ResolveStateAccess::class)
            element.resolveState = newPhase.asResolveState()
        }

        if (element is FirTypeParameterRefsOwner) {
            element.typeParameters.forEach { typeParameter ->
                // if it is not a type parameter of outer declaration
                if (typeParameter is FirTypeParameter) {
                    updatePhaseForNonLocals(typeParameter, newPhase, isTargetDeclaration = false)
                }
            }
        }

        when (element) {
            is FirScript -> element.forEachDependentDeclaration { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
            is FirFunction -> element.valueParameters.forEach { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
            is FirProperty -> {
                element.getter?.let { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
                element.setter?.let { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
                element.backingField?.let { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
            }
            else -> {}
        }
    }
}

private object PhaseUpdatingTransformer : FirVisitor<Unit, FirResolvePhase>() {
    override fun visitElement(element: FirElement, data: FirResolvePhase) {
        if (element is FirElementWithResolveState) {
            if (element.resolvePhase >= data &&
                element !is FirDefaultPropertyAccessor &&
                !(element is FirStatement && element.isScriptDependentDeclaration)
            ) return

            @OptIn(ResolveStateAccess::class)
            element.resolveState = data.asResolveState()
        }

        element.acceptChildren(this, data)
    }
}