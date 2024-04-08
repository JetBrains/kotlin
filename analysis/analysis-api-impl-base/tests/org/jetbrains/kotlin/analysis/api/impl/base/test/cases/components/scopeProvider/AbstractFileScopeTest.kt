/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractFileScopeTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        executeOnPooledThreadInReadAction {
            analyseForTest(mainFile) {
                val symbol = mainFile.getFileSymbol()
                val scope = symbol.getFileScope()

                val actual = with(DebugSymbolRenderer(renderExtra = true)) {
                    val renderedSymbol = render(analysisSession, symbol)
                    val renderedCallables = scope.getCallableSymbols().map { render(analysisSession, it) }
                    val renderedClassifiers = scope.getClassifierSymbols().map { render(analysisSession, it) }

                    "FILE SYMBOL:\n" + renderedSymbol + "\n" +
                            "\nCALLABLE SYMBOLS:\n" + renderedCallables.joinToString(separator = "\n\n", postfix = "\n") +
                            "\nCLASSIFIER SYMBOLS:\n" + renderedClassifiers.joinToString(separator = "\n\n")
                }
                testServices.assertions.assertEqualsToTestDataFileSibling(actual)

                val actualNames = prettyPrint { renderNamesContainedInScope(scope) }
                testServices.assertions.assertEqualsToTestDataFileSibling(actualNames, extension = ".names.txt")
            }
        }
    }
}
