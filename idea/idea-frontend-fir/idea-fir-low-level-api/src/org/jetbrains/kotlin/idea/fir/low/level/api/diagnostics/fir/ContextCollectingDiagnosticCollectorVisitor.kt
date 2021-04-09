/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.fir

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollectorVisitor
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.idea.fir.low.level.api.ContextByDesignationCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignation
import org.jetbrains.kotlin.idea.fir.low.level.api.api.collectDesignation

private class ContextCollectingDiagnosticCollectorVisitor private constructor(
    sessionHolder: SessionHolder,
    designation: FirDeclarationDesignation,
    firFile: FirFile,
) : AbstractDiagnosticCollectorVisitor(
    PersistentCheckerContextFactory.createEmptyPersistenceCheckerContext(sessionHolder),
    components = emptyList()
) {
    private val contextCollector = object : ContextByDesignationCollector<PersistentCheckerContext>(designation, firFile) {
        override fun getCurrentContext(): PersistentCheckerContext = context

        override fun goToNestedDeclaration(declaration: FirDeclaration) {
            declaration.accept(this@ContextCollectingDiagnosticCollectorVisitor, null)
        }
    }

    override fun goToNestedDeclarations(element: FirElement) {
        if (element is FirDeclaration) {
            contextCollector.nextStep()
        } else {
            element.accept(this, null)
        }
    }

    override fun runComponents(element: FirElement) {}

    companion object {
        fun collect(sessionHolder: SessionHolder, firFile: FirFile, designation: FirDeclarationDesignation): PersistentCheckerContext {
            val visitor = ContextCollectingDiagnosticCollectorVisitor(sessionHolder, designation, firFile)
            firFile.accept(visitor, null)
            return visitor.contextCollector.getCollectedContext()
        }
    }
}

internal object PersistenceContextCollector {
    fun collectContext(
        sessionHolder: SessionHolder,
        firFile: FirFile,
        declaration: FirDeclaration,
    ): PersistentCheckerContext {
        val isLocal = when (declaration) {
            is FirClassLikeDeclaration<*> -> declaration.symbol.classId.isLocal
            is FirCallableDeclaration<*> -> declaration.symbol.callableId.isLocal
            else -> error("Unsupported declaration ${declaration.renderWithType()}")
        }
        require(!isLocal) {
            "Cannot collect context for local declaration ${declaration.renderWithType()}"
        }
        val designation = declaration.collectDesignation()
        check(!designation.isLocalDesignation) {
            "Designation should not local for ${declaration.renderWithType()}"
        }
        return ContextCollectingDiagnosticCollectorVisitor.collect(sessionHolder, firFile, designation)
    }
}