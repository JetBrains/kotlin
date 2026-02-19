/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.BeforeElementDiagnosticCollectionHandler
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.ClassDiagnosticRetriever
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.beforeElementDiagnosticCollectionHandler
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir.PersistentCheckerContextFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.renderWithClassName
import org.jetbrains.kotlin.analysis.low.level.api.fir.withResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.useFirSessionConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
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
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Check that every declaration is visited exactly one time during diagnostic collection
 */
abstract class AbstractDiagnosticTraversalCounterTest : AbstractAnalysisApiBasedTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.apply {
            useFirSessionConfigurator { BeforeElementLLFirSessionConfigurator() }
        }
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        withResolutionFacade(mainFile) { resolutionFacade ->
            // we should get diagnostics before we resolve the whole file by  ktFile.getOrBuildFir
            mainFile.collectDiagnosticsForFile(resolutionFacade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)

            val firFile = mainFile.getOrBuildFirOfType<FirFile>(resolutionFacade)

            val errorElements = collectErrorElements(firFile)

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

    private fun collectErrorElements(firFile: FirFile): List<Pair<FirElement, Int>> {
        val handler = firFile.llFirSession.beforeElementDiagnosticCollectionHandler as BeforeElementTestDiagnosticCollectionHandler
        val errorElements = mutableListOf<Pair<FirElement, Int>>()
        val nonDuplicatingElements = findNonDuplicatingFirElements(firFile).filter { element ->
            when {
                element is FirTypeRef && element.source?.kind != KtRealSourceElementKind -> {
                    // AbstractDiagnosticCollectorVisitor do not visit such elements
                    false
                }
                element.source?.kind == KtRealSourceElementKind -> true
                ClassDiagnosticRetriever.shouldDiagnosticsAlwaysBeCheckedOn(element) -> true
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

    private class BeforeElementLLFirSessionConfigurator : LLFirSessionConfigurator {
        @OptIn(SessionConfiguration::class)
        override fun configure(session: LLFirSession) {
            val handler = BeforeElementTestDiagnosticCollectionHandler()
            session.register(BeforeElementDiagnosticCollectionHandler::class, handler)
        }
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

abstract class AbstractSourceDiagnosticTraversalCounterTest : AbstractDiagnosticTraversalCounterTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractScriptDiagnosticTraversalCounterTest : AbstractDiagnosticTraversalCounterTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}
