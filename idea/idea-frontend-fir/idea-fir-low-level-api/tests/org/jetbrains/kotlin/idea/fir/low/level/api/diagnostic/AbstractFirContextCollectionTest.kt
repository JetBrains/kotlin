/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostic

import com.intellij.openapi.util.io.FileUtil
import kotlinx.collections.immutable.PersistentList
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.PersistentImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getDiagnostics
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getFirFile
import org.jetbrains.kotlin.idea.fir.low.level.api.createResolveStateForNoCaching
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.BeforeElementDiagnosticCollectionHandler
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.fir.PersistenceContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.*
import org.jetbrains.kotlin.idea.fir.low.level.api.name
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

abstract class AbstractFirContextCollectionTest : KotlinLightCodeInsightFixtureTestCase() {
    fun doTest(path: String) {
        val testDataFile = File(path)
        val ktFile = myFixture.configureByText(testDataFile.name, FileUtil.loadFile(testDataFile)) as KtFile

        val handler = BeforeElementTestDiagnosticCollectionHandler()

        @OptIn(SessionConfiguration::class)
        val resolveState = createResolveStateForNoCaching(ktFile.getModuleInfo()) {
            register(BeforeElementDiagnosticCollectionHandler::class, handler)
        } as FirModuleResolveStateImpl

        val fileStructure = resolveState.fileStructureCache.getFileStructure(ktFile, resolveState.rootModuleSession.cache)
        val allStructureElements = fileStructure.getAllStructureElements()
        handler.elementsToCheckContext = allStructureElements.map { it.getFirDeclaration() }
        handler.firFile = ktFile.getFirFile(resolveState)
        ktFile.getDiagnostics(resolveState, DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
    }

    private fun FileStructureElement.getFirDeclaration(): FirDeclaration = when (this) {
        is NonReanalyzableDeclarationStructureElement -> fir
        is ReanalyzableStructureElement<*, *> -> firSymbol.fir as FirDeclaration
        is RootStructureElement -> firFile
    }


    private class BeforeElementTestDiagnosticCollectionHandler : BeforeElementDiagnosticCollectionHandler() {
        lateinit var elementsToCheckContext: List<FirDeclaration>
        lateinit var firFile: FirFile

        override fun beforeGoingNestedDeclaration(declaration: FirDeclaration, context: PersistentCheckerContext) {
            if (declaration is FirFile) {
                return
            }
            if (declaration in elementsToCheckContext) {
                val collectedContext = PersistenceContextCollector.collectContext(
                    SessionHolderImpl.createWithEmptyScopeSession(declaration.declarationSiteSession),
                    firFile,
                    declaration
                )
                compareStructurally(context, collectedContext)
            }
        }

        private fun compareStructurally(expected: PersistentCheckerContext, actual: PersistentCheckerContext) {
            assertEquals(expected.implicitReceiverStack.asString(), actual.implicitReceiverStack.asString())
            assertEquals(expected.containingDeclarations.asString(), actual.containingDeclarations.asString())
        }

        private fun PersistentImplicitReceiverStack.asString() =
            joinToString { it.boundSymbol.name() }

        private fun PersistentList<FirDeclaration>.asString() =
            joinToString(transform = FirDeclaration::name)
    }
}

