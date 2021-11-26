/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.BeforeElementDiagnosticCollectionHandler
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.SingleNonLocalDeclarationDiagnosticRetriever
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir.PersistentCheckerContextFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.renderWithClassName
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolveWithClearCaches
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollectorVisitor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Check that every declaration is visited exactly one time during diagnostic collection
 */
abstract class AbstractDiagnosticTraversalCounterTest  : AbstractLowLevelApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val handler = BeforeElementTestDiagnosticCollectionHandler()
        resolveWithClearCaches(
            ktFile,
            configureSession = {
                @OptIn(SessionConfiguration::class)
                register(BeforeElementDiagnosticCollectionHandler::class, handler)
            }
        ) { resolveState ->
            // we should get diagnostics before we resolve the whole file by  ktFile.getOrBuildFir
            ktFile.collectDiagnosticsForFile(resolveState, DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)

            val firFile = ktFile.getOrBuildFirOfType<FirFile>(resolveState)

            val errorElements = collectErrorElements(firFile, handler)

            if (errorElements.isNotEmpty()) {
                val zeroElements = errorElements.filter { it.second == 0 }
                val nonZeroElements = errorElements.filter { it.second > 1 }
                val message = buildString {
                    if (zeroElements.isNotEmpty()) {
                        appendLine(
                            """ |The following elements were not visited 
                            |${zeroElements.joinToString(separator = "\n\n") { it.first.source?.kind.toString() + " <> " + it.first.renderWithClassName() }}
                             """.trimMargin()
                        )
                    }
                    if (nonZeroElements.isNotEmpty()) {
                        appendLine(
                            """ |The following elements were visited more than one time
                            |${nonZeroElements.joinToString(separator = "\n\n") { it.second.toString() + " times " + it.first.source?.kind.toString() + " <> " + it.first.renderWithClassName() }}
                             """.trimMargin()
                        )
                    }
                }
                testServices.assertions.fail { message }
            }

        }
    }

    private fun collectErrorElements(
        firFile: FirElement,
        handler: BeforeElementTestDiagnosticCollectionHandler
    ): List<Pair<FirElement, Int>> {
        val errorElements = mutableListOf<Pair<FirElement, Int>>()
        val nonDuplicatingElements = findNonDuplicatingFirElements(firFile).filter { element ->
            when {
                element is FirTypeRef && element.source?.kind != KtRealSourceElementKind -> {
                    // AbstractDiagnosticCollectorVisitor do not visit such elements
                    false
                }
                element.source?.kind == KtRealSourceElementKind -> true
                SingleNonLocalDeclarationDiagnosticRetriever.shouldDiagnosticsAlwaysBeCheckedOn(element) -> true
                else -> false
            }
        }
        firFile.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                if (element !in nonDuplicatingElements) return
                val visitedTimes = handler.visitedTimes[element] ?: 0
                if (visitedTimes != 1) {
                    errorElements += element to visitedTimes
                }
                element.acceptChildren(this)
            }
        })
        return errorElements
    }


    private fun findNonDuplicatingFirElements(
        firFile: FirElement,
    ): Set<FirElement> {
        val elementUsageCount = mutableMapOf<FirElement, Int>()
        val sessionHolder = SessionHolderImpl((firFile as FirDeclaration).moduleData.session, ScopeSession())
        val visitor = object : AbstractDiagnosticCollectorVisitor(
            PersistentCheckerContextFactory.createEmptyPersistenceCheckerContext(sessionHolder)
        ) {
            override fun visitNestedElements(element: FirElement) {
                element.acceptChildren(this, null)
            }

            override fun checkElement(element: FirElement) {
                elementUsageCount.compute(element) { _, count -> (count ?: 0) + 1 }
            }
        }

        firFile.accept(visitor, null)
        return elementUsageCount.filterValues { it == 1 }.keys
    }


    class BeforeElementTestDiagnosticCollectionHandler : BeforeElementDiagnosticCollectionHandler() {
        val visitedTimes = mutableMapOf<FirElement, Int>()
        override fun beforeCollectingForElement(element: FirElement) {
            if (!visitedTimes.containsKey(element)) {
                visitedTimes[element] = 1
            } else {
                visitedTimes.compute(element) { _, count -> (count ?: 0) + 1 }
            }
        }
    }
}
