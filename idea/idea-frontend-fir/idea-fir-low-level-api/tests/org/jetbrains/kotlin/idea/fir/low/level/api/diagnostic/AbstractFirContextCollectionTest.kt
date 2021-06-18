/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostic

import kotlinx.collections.immutable.PersistentList
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.PersistentImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getDiagnostics
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirFile
import org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based.FrontendApiSingleTestDataFileTest
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.BeforeElementDiagnosticCollectionHandler
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.fir.PersistenceContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FileStructureElement
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.NonReanalyzableDeclarationStructureElement
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.ReanalyzableStructureElement
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.RootStructureElement
import org.jetbrains.kotlin.idea.fir.low.level.api.name
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSession
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractFirContextCollectionTest : FrontendApiSingleTestDataFileTest() {
    private var handler: BeforeElementTestDiagnosticCollectionHandler? = null

    @OptIn(SessionConfiguration::class)
    override fun FirIdeSession.configureSession() {
        handler = BeforeElementTestDiagnosticCollectionHandler()
        register(BeforeElementDiagnosticCollectionHandler::class, handler!!)
    }

    override fun doTest(ktFile: KtFile, module: TestModule, resolveState: FirModuleResolveState, testServices: TestServices) {
        check(resolveState is FirModuleResolveStateImpl)

        val fileStructure = resolveState.fileStructureCache.getFileStructure(ktFile, resolveState.rootModuleSession.cache)
        val allStructureElements = fileStructure.getAllStructureElements()
        handler!!.assertions = testServices.assertions
        handler!!.elementsToCheckContext = allStructureElements.map { it.getFirDeclaration() }
        handler!!.firFile = ktFile.getOrBuildFirFile(resolveState)
        ktFile.getDiagnostics(resolveState, DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
        handler = null
    }

    private fun FileStructureElement.getFirDeclaration(): FirDeclaration = when (this) {
        is NonReanalyzableDeclarationStructureElement -> fir
        is ReanalyzableStructureElement<*, *> -> firSymbol.fir as FirDeclaration
        is RootStructureElement -> firFile
    }


    private class BeforeElementTestDiagnosticCollectionHandler() : BeforeElementDiagnosticCollectionHandler() {
        lateinit var assertions: AssertionsService
        lateinit var elementsToCheckContext: List<FirDeclaration>
        lateinit var firFile: FirFile

        override fun beforeGoingNestedDeclaration(declaration: FirDeclaration, context: PersistentCheckerContext) {
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

        private fun compareStructurally(expected: PersistentCheckerContext, actual: PersistentCheckerContext) {
            assertions.assertEquals(expected.implicitReceiverStack.asString(), actual.implicitReceiverStack.asString())
            assertions.assertEquals(expected.containingDeclarations.asString(), actual.containingDeclarations.asString())
        }

        private fun PersistentImplicitReceiverStack.asString() =
            joinToString { it.boundSymbol.name() }

        private fun PersistentList<FirDeclaration>.asString() =
            joinToString(transform = FirDeclaration::name)
    }
}

