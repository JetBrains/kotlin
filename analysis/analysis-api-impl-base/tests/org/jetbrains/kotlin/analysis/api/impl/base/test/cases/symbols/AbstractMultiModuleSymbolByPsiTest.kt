/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractMultiModuleSymbolByPsiTest : AbstractAnalysisApiBasedTest() {
    override fun doTest(testServices: TestServices) {
        val files = testServices.ktTestModuleStructure.allMainKtFiles
        val debugRenderer = DebugSymbolRenderer()

        val debugPrinter = PrettyPrinter()
        val prettyPrinter = PrettyPrinter(indentSize = 4)

        for (file in files) {
            val fileDirective = "// FILE: ${file.name}\n"
            debugPrinter.appendLine(fileDirective)
            prettyPrinter.appendLine(fileDirective)

            analyseForTest(file) {
                val fileSymbol = file.symbol
                file.forEachDescendantOfType<KtDeclaration>(predicate = { it.isValidForSymbolCreation }) { declaration ->
                    val symbol = declaration.symbol

                    checkContainingFileSymbol(fileSymbol, symbol, testServices)

                    debugPrinter.appendLine(debugRenderer.render(useSiteSession, symbol))
                    debugPrinter.appendLine()

                    prettyPrinter.withIndents(indentCount = declaration.parentsOfType<KtDeclaration>(withSelf = false).count()) {
                        prettyPrinter.appendLine(symbol.render(KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES))
                        prettyPrinter.appendLine()
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(debugPrinter.toString())
        testServices.assertions.assertEqualsToTestDataFileSibling(prettyPrinter.toString(), extension = ".pretty.txt")
    }

    /**
     * Processes the descendants of the element using the preorder implementation of tree traversal.
     */
    private inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfType(
        noinline predicate: (T) -> Boolean = { true },
        noinline action: (T) -> Unit,
    ) = this.accept(object : PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is T && predicate(element)) {
                action(element)
            }
            super.visitElement(element)
        }
    })
}