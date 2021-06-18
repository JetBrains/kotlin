/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostic

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirRealSourceElementKind
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollectorVisitor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFir
import org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based.FrontendApiSingleTestDataFileTest
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.BeforeElementDiagnosticCollectionHandler
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.SingleNonLocalDeclarationDiagnosticRetriever
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.fir.PersistentCheckerContextFactory
import org.jetbrains.kotlin.idea.fir.low.level.api.renderWithClassName
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSession
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Check that every declaration is visited exactly one time during diagnostic collection
 */
abstract class AbstractDiagnosticTraversalCounterTest : FrontendApiSingleTestDataFileTest() {
    private var handler: BeforeElementTestDiagnosticCollectionHandler? = null

    @OptIn(SessionConfiguration::class)
    override fun FirIdeSession.configureSession() {
        handler = BeforeElementTestDiagnosticCollectionHandler()
        register(BeforeElementDiagnosticCollectionHandler::class, handler!!)
    }

    override fun doTest(ktFile: KtFile, module: TestModule, resolveState: FirModuleResolveState, testServices: TestServices) {
        // we should get diagnostics before we resolve the whole file by  ktFile.getOrBuildFir
        ktFile.collectDiagnosticsForFile(resolveState, DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)

        val firFile = ktFile.getOrBuildFir(resolveState)

        val errorElements = collectErrorElements(firFile, handler!!)

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
        handler = null
    }

    private fun collectErrorElements(
        firFile: FirElement,
        handler: BeforeElementTestDiagnosticCollectionHandler
    ): List<Pair<FirElement, Int>> {
        val errorElements = mutableListOf<Pair<FirElement, Int>>()
        val nonDuplicatingElements = findNonDuplicatingFirElements(firFile).filter { element ->
            when {
                element is FirTypeRef && element.source?.kind != FirRealSourceElementKind -> {
                    // AbstractDiagnosticCollectorVisitor do not visit such elements
                    false
                }
                element.source?.kind == FirRealSourceElementKind -> true
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
