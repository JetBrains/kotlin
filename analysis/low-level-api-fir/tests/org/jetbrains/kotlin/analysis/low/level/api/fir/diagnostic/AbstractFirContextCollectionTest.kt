/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic

import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirSourceResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getDiagnostics
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.BeforeElementDiagnosticCollectionHandler
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir.PersistenceContextCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.DanglingTopLevelModifierListStructureElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructureElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.NonReanalyzableDeclarationStructureElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.ReanalyzableStructureElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.RootStructureElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.name
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolveWithClearCaches
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions


abstract class AbstractFirContextCollectionTest : AbstractLowLevelApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val handler = BeforeElementTestDiagnosticCollectionHandler(testServices.assertions)
        resolveWithClearCaches(
            ktFile,
            configureSession = {
                @OptIn(SessionConfiguration::class)
                register(BeforeElementDiagnosticCollectionHandler::class, handler)
            }
        ) { firResolveSession ->
            check(firResolveSession is LLFirSourceResolveSession)

            val session = firResolveSession.getSessionFor(ktFile.getKtModule()) as LLFirResolvableModuleSession
            val fileStructureCache = session.moduleComponents.fileStructureCache

            val fileStructure = fileStructureCache.getFileStructure(ktFile)
            val allStructureElements = fileStructure.getAllStructureElements()

            handler.elementsToCheckContext = allStructureElements.map { it.getFirDeclaration() }
            handler.firFile = ktFile.getOrBuildFirFile(firResolveSession)

            ktFile.getDiagnostics(firResolveSession, DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
        }
    }

    private fun FileStructureElement.getFirDeclaration(): FirDeclaration = when (this) {
        is NonReanalyzableDeclarationStructureElement -> fir
        is ReanalyzableStructureElement<*, *> -> firSymbol.fir
        is RootStructureElement -> firFile
        is DanglingTopLevelModifierListStructureElement -> fir
    }


    private class BeforeElementTestDiagnosticCollectionHandler(
        private val assertions: AssertionsService
    ) :
        BeforeElementDiagnosticCollectionHandler() {
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

