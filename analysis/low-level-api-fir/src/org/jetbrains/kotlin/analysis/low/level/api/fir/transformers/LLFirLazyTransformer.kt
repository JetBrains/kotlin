/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.visitors.FirVisitor

internal interface LLFirLazyTransformer {
    fun transformDeclaration(phaseRunner: LLFirPhaseRunner)

    fun checkIsResolved(target: FirElementWithResolveState)

    fun checkNestedDeclarationsAreResolved(target: FirElementWithResolveState) {
        if (target !is FirDeclaration) return
        checkFunctionParametersAreResolved(target)
        checkPropertyAccessorsAreResolved(target)
        checkClassMembersAreResolved(target)
    }

    fun checkClassMembersAreResolved(declaration: FirDeclaration) {
        if (!needCheckingIfClassMembersAreResolved) return
        if (declaration is FirClass) {
            for (member in declaration.declarations) {
                checkClassMembersAreResolved(member)
            }
        }
    }

    fun checkPropertyAccessorsAreResolved(declaration: FirDeclaration) {
        if (declaration is FirProperty) {
            declaration.getter?.let { checkIsResolved(it) }
            declaration.setter?.let { checkIsResolved(it) }
        }
    }


    fun checkFunctionParametersAreResolved(declaration: FirDeclaration) {
        if (declaration is FirFunction) {
            for (parameter in declaration.valueParameters) {
                checkIsResolved(parameter)
            }
        }
    }

    fun checkTypeParametersAreResolved(declaration: FirDeclaration) {
        if (declaration is FirTypeParameterRefsOwner) {
            for (parameter in declaration.typeParameters) {
                if (parameter is FirTypeParameter) {
                    checkIsResolved(parameter)
                }
            }
        }
    }

    companion object {
        private object WholeTreePhaseUpdater : FirVisitor<Unit, FirResolvePhase>() {
            override fun visitElement(element: FirElement, data: FirResolvePhase) {
                if (element is FirElementWithResolveState) {
                    if (element.resolvePhase >= data && element !is FirDefaultPropertyAccessor) return

                    @OptIn(ResolveStateAccess::class)
                    element.resolveState = data.asResolveState()
                }

                element.acceptChildren(this, data)
            }
        }

        private fun updatePhaseForNonLocals(element: FirElementWithResolveState, newPhase: FirResolvePhase) {
            if (element.resolvePhase >= newPhase) return

            @OptIn(ResolveStateAccess::class)
            element.resolveState = newPhase.asResolveState()

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

        fun updatePhaseDeep(element: FirElementWithResolveState, newPhase: FirResolvePhase, withNonLocalDeclarations: Boolean = false) {
            if (withNonLocalDeclarations) {
                WholeTreePhaseUpdater.visitElement(element, newPhase)
            } else {
                updatePhaseForNonLocals(element, newPhase)
            }
        }

        internal var needCheckingIfClassMembersAreResolved: Boolean = false
            @TestOnly set

        val DUMMY = object : LLFirLazyTransformer {
            override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) = Unit
            override fun checkIsResolved(target: FirElementWithResolveState) = error("Not implemented")
        }
    }
}
