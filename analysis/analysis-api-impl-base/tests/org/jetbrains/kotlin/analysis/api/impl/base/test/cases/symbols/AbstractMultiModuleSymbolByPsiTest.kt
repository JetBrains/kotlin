/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KtDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractMultiModuleSymbolByPsiTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByModuleStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val modules = moduleStructure.modules
        val files = modules.flatMap { testServices.ktModuleProvider.getModuleFiles(it).filterIsInstance<KtFile>() }

        val debugRenderer = DebugSymbolRenderer()

        val debugPrinter = PrettyPrinter()
        val prettyPrinter = PrettyPrinter()

        for (file in files) {
            val fileDirective = "// FILE: ${file.name}\n"
            debugPrinter.appendLine(fileDirective)
            prettyPrinter.appendLine(fileDirective)

            analyseForTest(file) {
                file.collectDescendantsOfType<KtDeclaration> { it.isValidForSymbolCreation }.forEach { declaration ->
                    val symbol = declaration.getSymbol()

                    debugPrinter.appendLine(debugRenderer.render(symbol))
                    debugPrinter.appendLine()

                    prettyPrinter.appendLine(symbol.render(KtDeclarationRendererForDebug.WITH_QUALIFIED_NAMES))
                    prettyPrinter.appendLine()
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(debugPrinter.toString())
        testServices.assertions.assertEqualsToTestDataFileSibling(prettyPrinter.toString(), extension = ".pretty.txt")
    }
}