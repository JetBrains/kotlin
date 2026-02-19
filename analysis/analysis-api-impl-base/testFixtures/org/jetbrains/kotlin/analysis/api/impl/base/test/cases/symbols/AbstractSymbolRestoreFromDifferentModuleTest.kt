/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.symbols.KaDebugRenderer
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSymbolRestoreFromDifferentModuleTest : AbstractAnalysisApiBasedTest() {
    private val defaultRenderer = KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES

    override fun doTest(testServices: TestServices) {
        val declaration =
            testServices.expressionMarkerProvider.getBottommostElementsOfTypeAtCarets<KtDeclaration>(testServices).single().first

        val restoreAt =
            testServices.expressionMarkerProvider.getBottommostElementsOfTypeAtCarets<KtElement>(
                testServices,
                qualifier = "restoreAt"
            ).single().first

        val project = declaration.project
        val declarationModule = KotlinProjectStructureProvider.getModule(project, declaration, useSiteModule = null)
        val restoreAtModule = KotlinProjectStructureProvider.getModule(project, restoreAt, useSiteModule = null)

        val (debugRendered, prettyRendered, pointer) = analyzeForTest(declaration) {
            val symbol = declaration.symbol
            val pointer = symbol.createPointer()
            Triple(KaDebugRenderer().render(useSiteSession, symbol), symbol.render(defaultRenderer), pointer)
        }
        configurator.doGlobalModuleStateModification(project)

        val (debugRenderedRestored, prettyRenderedRestored) = analyzeForTest(restoreAt) {
            val symbol = pointer.restoreSymbol()
            symbol?.let { KaDebugRenderer().render(useSiteSession, it) } to symbol?.render(defaultRenderer)
        }

        val actualDebug = prettyPrint {
            appendLine("Inital from ${declarationModule.moduleDescription}:")
            appendLine(debugRendered)
            appendLine()
            appendLine("Restored in ${restoreAtModule.moduleDescription}:")
            appendLine(debugRenderedRestored ?: NOT_RESTORED)
        }
        testServices.assertions.assertEqualsToTestOutputFile(actualDebug)

        val actualPretty = prettyPrint {
            appendLine("Inital from ${declarationModule.moduleDescription}:")
            appendLine(prettyRendered)
            appendLine()
            appendLine("Restored in ${restoreAtModule.moduleDescription}:")
            appendLine(prettyRenderedRestored ?: NOT_RESTORED)
        }
        testServices.assertions.assertEqualsToTestOutputFile(actualPretty, extension = ".pretty.txt")
    }

    companion object {
        private const val NOT_RESTORED = "<NOT RESTORED>"
    }
}

