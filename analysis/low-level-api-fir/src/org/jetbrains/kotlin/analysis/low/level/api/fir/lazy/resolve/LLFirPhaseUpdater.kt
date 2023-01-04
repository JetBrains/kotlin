/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.visitors.FirVisitor

internal object LLFirPhaseUpdater {
    fun updateDeclarationInternalsPhase(
        target: FirElementWithResolveState,
        newPhase: FirResolvePhase,
        updateForLocalDeclarations: Boolean,
    ) {
        if (updateForLocalDeclarations) {
            PhaseUpdatingTransformer.visitElement(target, newPhase)
        } else {
            updatePhaseForNonLocals(target, newPhase)
        }
    }


    private fun updatePhaseForNonLocals(element: FirElementWithResolveState, newPhase: FirResolvePhase) {
        if (element.resolvePhase >= newPhase) return
        element.replaceResolveState(newPhase.asResolveState())

        if (element is FirTypeParameterRefsOwner) {
            element.typeParameters.forEach { typeParameter ->
                // if it is not a type parameter of outer declaration
                if (typeParameter is FirTypeParameter) {
                    updatePhaseForNonLocals(typeParameter, newPhase)
                }
            }
        }

        when (element) {
            is FirFunction -> {
                element.valueParameters.forEach { updatePhaseForNonLocals(it, newPhase) }
            }
            is FirProperty -> {
                element.getter?.let { updatePhaseForNonLocals(it, newPhase) }
                element.setter?.let { updatePhaseForNonLocals(it, newPhase) }
            }
            is FirClass -> {
                element.declarations.forEach {
                    updatePhaseForNonLocals(it, newPhase)
                }
            }
            else -> Unit
        }
    }
}

private object PhaseUpdatingTransformer : FirVisitor<Unit, FirResolvePhase>() {
    override fun visitElement(element: FirElement, data: FirResolvePhase) {
        if (element is FirElementWithResolveState) {
            if (element.resolvePhase >= data && element !is FirDefaultPropertyAccessor) return
            element.replaceResolveState(data.asResolveState())
        }

        element.acceptChildren(this, data)
    }
}