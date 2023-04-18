/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getDiagnostics
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.BeforeElementDiagnosticCollectionHandler
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.beforeElementDiagnosticCollectionHandler
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir.PersistenceContextCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.isSourceSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.name
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolveWithClearCaches
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.useFirSessionConfigurator
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.*

abstract class AbstractFirContextCollectionTest : AbstractLowLevelApiSingleFileTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.apply {
            useFirSessionConfigurator(::BeforeElementLLFirSessionConfigurator)
        }
    }

    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        resolveWithClearCaches(ktFile) { firResolveSession ->
            check(firResolveSession.isSourceSession)

            val module = firResolveSession.getModule(ktFile)
            val session = firResolveSession.getSessionFor(module) as LLFirResolvableModuleSession
            val handler = session.beforeElementDiagnosticCollectionHandler as BeforeElementTestDiagnosticCollectionHandler

            val fileStructureCache = session.moduleComponents.fileStructureCache

            val fileStructure = fileStructureCache.getFileStructure(ktFile)
            val allStructureElements = fileStructure.getAllStructureElements()

            handler.elementsToCheckContext = allStructureElements.map { it.getFirDeclaration() }
            handler.firFile = ktFile.getOrBuildFirFile(firResolveSession)

            ktFile.getDiagnostics(firResolveSession, DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
        }
    }

    private fun FileStructureElement.getFirDeclaration(): FirDeclaration = when (this) {
        is ReanalyzableStructureElement<*, *> -> firSymbol.fir
        is RootStructureElement -> firFile
        is DanglingTopLevelModifierListStructureElement -> fir
        is NonReanalyzableClassDeclarationStructureElement -> fir
        is NonReanalyzableNonClassDeclarationStructureElement -> fir
    }

    private class BeforeElementLLFirSessionConfigurator(private val testServices: TestServices) : LLFirSessionConfigurator {
        @OptIn(SessionConfiguration::class)
        override fun configure(session: LLFirSession) {
            val handler = BeforeElementTestDiagnosticCollectionHandler(testServices.assertions)
            session.register(BeforeElementDiagnosticCollectionHandler::class, handler)
        }
    }

    private class BeforeElementTestDiagnosticCollectionHandler(
        private val assertions: AssertionsService
    ) : BeforeElementDiagnosticCollectionHandler() {
        lateinit var elementsToCheckContext: List<FirDeclaration>
        lateinit var firFile: FirFile

        override fun beforeGoingNestedDeclaration(declaration: FirDeclaration, context: CheckerContext) {
            if (declaration is FirFile) {
                return
            }
            if (declaration in elementsToCheckContext) {
                val collectedContext = PersistenceContextCollector.collectContext(
                    SessionHolderImpl.createWithEmptyScopeSession(declaration.moduleData.session),
                    firFile,
                    declaration
                )
                compareStructurally(context, collectedContext)
            }
        }

        private fun compareStructurally(expected: CheckerContext, actual: CheckerContext) {
            assertions.assertEquals(expected.implicitReceiverStack.asString(), actual.implicitReceiverStack.asString())
            assertions.assertEquals(expected.containingDeclarations.asString(), actual.containingDeclarations.asString())
        }

        private fun ImplicitReceiverStack.asString() =
            joinToString { it.boundSymbol.name() }

        private fun List<FirDeclaration>.asString() =
            joinToString(transform = FirDeclaration::name)
    }
}

abstract class AbstractFirSourceContextCollectionTest : AbstractFirContextCollectionTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractFirOutOfContentRootContextCollectionTest : AbstractFirContextCollectionTest() {
    override val configurator = AnalysisApiFirOutOfContentRootTestConfigurator
}