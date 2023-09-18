/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.ContextByDesignationCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.containingClassId
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContextForProvider
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollectorVisitor
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

private class ContextCollectingDiagnosticCollectorVisitor private constructor(
    sessionHolder: SessionHolder,
    designation: FirDesignationWithFile,
) : AbstractDiagnosticCollectorVisitor(
    PersistentCheckerContextFactory.createEmptyPersistenceCheckerContext(sessionHolder)
) {
    private val contextCollector = object : ContextByDesignationCollector<CheckerContextForProvider>(designation) {
        override fun getCurrentContext(): CheckerContextForProvider = context

        override fun goToNestedDeclaration(target: FirElementWithResolveState) {
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
        fun collect(sessionHolder: SessionHolder, designation: FirDesignationWithFile): CheckerContextForProvider {
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
    ): CheckerContextForProvider {
        val isLocal = when (declaration) {
            is FirClassLikeDeclaration -> declaration.symbol.classId.isLocal
            is FirCallableDeclaration -> declaration.symbol.callableId.isLocal
            is FirDanglingModifierList -> declaration.containingClass()?.classId?.isLocal == true
            is FirAnonymousInitializer -> declaration.containingClassId().isLocal
            is FirScript, is FirCodeFragment -> false
            else -> errorWithAttachment("Unsupported declaration ${declaration::class}") {
                withFirEntry("declaration", declaration)
            }
        }

        requireWithAttachment(
            !isLocal,
            { "Cannot collect context for local declaration ${declaration::class}" }
        ) {
            withFirEntry("declaration", declaration)
        }

        val designation = declaration.collectDesignation(firFile)
        designation.path.forEach { firClass ->
            firClass.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        }

        return ContextCollectingDiagnosticCollectorVisitor.collect(sessionHolder, designation)
    }
}
