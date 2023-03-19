/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KtDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.test.services.TestModuleStructure

abstract class AbstractSymbolRestoreFromDifferentModuleTest : AbstractAnalysisApiBasedTest() {
    private val defaultRenderer = KtDeclarationRendererForDebug.WITH_QUALIFIED_NAMES

    override fun doTestByModuleStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val declaration =
            testServices.expressionMarkerProvider.getElementsOfTypeAtCarets<KtDeclaration>(moduleStructure, testServices).single().first

        val restoreAt =
            testServices.expressionMarkerProvider.getElementsOfTypeAtCarets<KtElement>(
                moduleStructure,
                testServices,
                caretTag = "restoreAt"
            ).single().first

        val (debugRendered, prettyRendered, pointer) = analyseForTest(declaration) {
            val symbol = declaration.getSymbol()
            val pointer = symbol.createPointer()
            Triple(DebugSymbolRenderer().render(symbol), symbol.render(defaultRenderer), pointer)
        }
        configurator.doOutOfBlockModification(declaration.containingKtFile)

        val (debugRenderedRestored, prettyRenderedRestored) = analyseForTest(restoreAt) {
            val symbol = pointer.restoreSymbol() as? KtDeclarationSymbol
            symbol?.let { DebugSymbolRenderer().render(it) } to symbol?.render(defaultRenderer)
        }

        val actualDebug = prettyPrint {
            appendLine("Inital from ${declaration.getKtModule().moduleDescription}:")
            appendLine(debugRendered)
            appendLine()
            appendLine("Restored in ${restoreAt.getKtModule().moduleDescription}:")
            appendLine(debugRenderedRestored ?: NOT_RESTORED)
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actualDebug)

        val actualPretty = prettyPrint {
            appendLine("Inital from ${declaration.getKtModule().moduleDescription}:")
            appendLine(prettyRendered)
            appendLine()
            appendLine("Restored in ${restoreAt.getKtModule().moduleDescription}:")
            appendLine(prettyRenderedRestored ?: NOT_RESTORED)
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actualPretty, extension = ".pretty.txt")
    }

    companion object {
        private const val NOT_RESTORED = "<NOT RESTORED>"
    }
}

