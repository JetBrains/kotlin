/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.ContextByDesignationCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignation
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolvePhase
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollectorVisitor
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.SessionHolder

private class ContextCollectingDiagnosticCollectorVisitor private constructor(
    sessionHolder: SessionHolder,
    designation: FirDesignationWithFile,
) : AbstractDiagnosticCollectorVisitor(
    PersistentCheckerContextFactory.createEmptyPersistenceCheckerContext(sessionHolder)
) {
    private val contextCollector = object : ContextByDesignationCollector<CheckerContext>(designation) {
        override fun getCurrentContext(): CheckerContext = context

        override fun goToNestedDeclaration(target: FirElementWithResolvePhase) {
            target.accept(this@ContextCollectingDiagnosticCollectorVisitor, null)
        }
    }

    override fun visitNestedElements(element: FirElement) {
        if (element is FirDeclaration) {
            contextCollector.nextStep()
        } else {
            element.accept(this, null)
        }
    }

    override fun checkElement(element: FirElement) {}

    companion object {
        fun collect(sessionHolder: SessionHolder, designation: FirDesignationWithFile): CheckerContext {
            val visitor = ContextCollectingDiagnosticCollectorVisitor(sessionHolder, designation)
            designation.firFile.accept(visitor, null)
            return visitor.contextCollector.getCollectedContext()
        }
    }
}

internal object PersistenceContextCollector {
    fun collectContext(
        sessionHolder: SessionHolder,
        firFile: FirFile,
        declaration: FirDeclaration,
    ): CheckerContext {
        val isLocal = when (declaration) {
            is FirClassLikeDeclaration -> declaration.symbol.classId.isLocal
            is FirCallableDeclaration -> declaration.symbol.callableId.isLocal
            is FirDanglingModifierList -> declaration.containingClass()?.classId?.isLocal == true
            else -> error("Unsupported declaration ${declaration.renderWithType()}")
        }
        require(!isLocal) {
            "Cannot collect context for local declaration ${declaration.renderWithType()}"
        }
        val designation = declaration.collectDesignation(firFile)
        return ContextCollectingDiagnosticCollectorVisitor.collect(sessionHolder, designation)
    }
}
