/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.scopes

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractFileScopeTest : AbstractAnalysisApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val actual = executeOnPooledThreadInReadAction {
            analyseForTest(ktFile) {
                val symbol = ktFile.getFileSymbol()
                val scope = symbol.getFileScope()
                with(DebugSymbolRenderer(renderExtra = true)) {
                    val renderedSymbol = render(symbol)
                    val callableNames = scope.getPossibleCallableNames()
                    val renderedCallables = scope.getCallableSymbols().map { render(it) }
                    val classifierNames = scope.getPossibleClassifierNames()
                    val renderedClassifiers = scope.getClassifierSymbols().map { render(it) }

                    "FILE SYMBOL:\n" + renderedSymbol + "\n" +
                            "\nCALLABLE NAMES:\n" + callableNames.joinToString(prefix = "[", postfix = "]\n", separator = ", ") +
                            "\nCALLABLE SYMBOLS:\n" + renderedCallables.joinToString(separator = "\n\n", postfix = "\n") +
                            "\nCLASSIFIER NAMES:\n" + classifierNames.joinToString(prefix = "[", postfix = "]\n", separator = ", ") +
                            "\nCLASSIFIER SYMBOLS:\n" + renderedClassifiers.joinToString(separator = "\n\n")
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}