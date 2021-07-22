/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignation

internal interface FirLazyTransformerForIDE {
    fun transformDeclaration(phaseRunner: FirPhaseRunner)
    fun ensureResolved(declaration: FirDeclaration)
    fun ensureResolvedDeep(declaration: FirDeclaration) {
        if (!enableDeepEnsure) return
        ensureResolved(declaration)
        if (declaration is FirRegularClass) {
            declaration.declarations.forEach(::ensureResolvedDeep)
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

            when (element) {
                is FirProperty -> {
                    element.getter?.run { if (resolvePhase < newPhase) replaceResolvePhase(newPhase) }
                    element.setter?.run { if (resolvePhase < newPhase) replaceResolvePhase(newPhase) }
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

        val DUMMY = object : FirLazyTransformerForIDE {
            override fun transformDeclaration(phaseRunner: FirPhaseRunner) = Unit
            override fun ensureResolved(declaration: FirDeclaration) = error("Not implemented")
        }
    }
}
