/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.visitors.FirVisitor

internal interface LLFirLazyTransformer {
    fun transformDeclaration(phaseRunner: LLFirPhaseRunner)

    fun checkIsResolved(declaration: FirDeclaration)

    fun checkIsResolvedDeep(declaration: FirDeclaration) {
        if (!enableDeepEnsure) return
        checkIsResolved(declaration)
        if (declaration is FirRegularClass) {
            declaration.declarations.forEach(::checkIsResolvedDeep)
        }
    }

    companion object {
        private object WholeTreePhaseUpdater : FirVisitor<Unit, FirResolvePhase>() {
            override fun visitElement(element: FirElement, data: FirResolvePhase) {
                if (element is FirDeclaration) {
                    if (element.resolvePhase >= data && element !is FirDefaultPropertyAccessor) return
                    element.replaceResolvePhase(data)
                }
                element.acceptChildren(this, data)
            }
        }

        private fun updatePhaseForNonLocals(element: FirDeclaration, newPhase: FirResolvePhase) {
            if (element.resolvePhase >= newPhase) return
            element.replaceResolvePhase(newPhase)

            if (element is FirCallableDeclaration) {
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

        fun updatePhaseDeep(element: FirDeclaration, newPhase: FirResolvePhase, withNonLocalDeclarations: Boolean = false) {
            if (withNonLocalDeclarations) {
                WholeTreePhaseUpdater.visitElement(element, newPhase)
            } else {
                updatePhaseForNonLocals(element, newPhase)
            }
        }

        internal var enableDeepEnsure: Boolean = false
            @TestOnly set

        val DUMMY = object : LLFirLazyTransformer {
            override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) = Unit
            override fun checkIsResolved(declaration: FirDeclaration) = error("Not implemented")
        }
    }
}
