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
import org.jetbrains.kotlin.idea.fir.low.level.api.util.collectDesignation

private class ContextCollectingDiagnosticCollectorVisitor private constructor(
    sessionHolder: SessionHolder,
    private val designation: Iterator<FirDeclaration>,
) : AbstractDiagnosticCollectorVisitor(
    PersistentCheckerContextFactory.createEmptyPersistenceCheckerContext(sessionHolder),
    components = emptyList()
) {
    private var savedContext: PersistentCheckerContext? = null

    override fun goToNestedDeclarations(element: FirElement) {
        if (designation.hasNext()) {
            designation.next().accept(this, null)
        } else {
            savedContext = context
        }
    }

    override fun runComponents(element: FirElement) {}

    companion object {
        fun collect(sessionHolder: SessionHolder, firFile: FirFile, designation: List<FirDeclaration>): PersistentCheckerContext {
            val visitor = ContextCollectingDiagnosticCollectorVisitor(sessionHolder, designation.iterator())
            firFile.accept(visitor, null)
            return visitor.savedContext
                ?: error("Context was not saved")
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
        return ContextCollectingDiagnosticCollectorVisitor.collect(sessionHolder, firFile, designation)
    }

}