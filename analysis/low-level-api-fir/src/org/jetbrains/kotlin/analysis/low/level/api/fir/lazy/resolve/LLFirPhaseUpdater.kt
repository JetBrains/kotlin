/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

object LLFirPhaseUpdater {
    fun updateDeclarationInternalsPhase(target: FirElementWithResolveState, newPhase: FirResolvePhase) {
        updatePhaseForNonLocals(target, newPhase, isTargetDeclaration = true)

        if (newPhase == FirResolvePhase.BODY_RESOLVE) {
            when (target) {
                is FirConstructor -> {
                    target.delegatedConstructor?.accept(LocalElementPhaseUpdatingTransformer)
                    updateFunctionLocalElements(target)
                }

                is FirFunction -> updateFunctionLocalElements(target)
                is FirVariable -> {
                    target.initializer?.accept(LocalElementPhaseUpdatingTransformer)
                    target.delegate?.accept(LocalElementPhaseUpdatingTransformer)
                    target.getter?.let(::updateFunctionLocalElements)
                    target.setter?.let(::updateFunctionLocalElements)
                    target.backingField?.initializer?.accept(LocalElementPhaseUpdatingTransformer)
                }

                is FirAnonymousInitializer -> target.body?.accept(LocalElementPhaseUpdatingTransformer)
                is FirCodeFragment -> target.block.accept(LocalElementPhaseUpdatingTransformer)
                is FirDanglingModifierList -> target.acceptChildren(LocalElementPhaseUpdatingTransformer)
            }
        }
    }

    fun updateFunctionLocalElements(target: FirFunction) {
        target.body?.accept(LocalElementPhaseUpdatingTransformer)
        target.valueParameters.forEach { it.defaultValue?.accept(LocalElementPhaseUpdatingTransformer) }
    }

    fun updatePhaseForNonLocals(element: FirElementWithResolveState, newPhase: FirResolvePhase, isTargetDeclaration: Boolean) {
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
            is FirRegularClass -> {
                element.contextParameters.forEach { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
            }
            is FirScript -> {
                element.receivers.forEach { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
            }
            is FirFunction -> {
                element.valueParameters.forEach { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
                element.receiverParameter?.let { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
                element.contextParameters.forEach { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
            }
            is FirProperty -> {
                element.getter?.let { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
                element.setter?.let { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
                element.backingField?.let { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
                element.receiverParameter?.let { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
                element.contextParameters.forEach { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
            }
            else -> {}
        }
    }
}

object LocalElementPhaseUpdatingTransformer : FirVisitorVoid() {
    override fun visitElement(element: FirElement) {
        if (element is FirElementWithResolveState) {
            @OptIn(ResolveStateAccess::class)
            element.resolveState = FirResolvePhase.BODY_RESOLVE.asResolveState()
        }

        element.acceptChildren(this)
    }
}
