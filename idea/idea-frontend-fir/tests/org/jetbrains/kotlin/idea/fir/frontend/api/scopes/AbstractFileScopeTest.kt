/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.frontend.api.scopes

import org.jetbrains.kotlin.idea.fir.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.fir.test.framework.AbstractKtIdeaTestWithSingleTestFileTest
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.io.File

abstract class AbstractFileScopeTest : AbstractKtIdeaTestWithSingleTestFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val actual = executeOnPooledThreadInReadAction {
            analyse(ktFile) {
                val symbol = ktFile.getFileSymbol()
                val scope = symbol.getFileScope()

                val renderedSymbol = DebugSymbolRenderer.render(symbol)
                val callableNames = scope.getPossibleCallableNames()
                val renderedCallables = scope.getCallableSymbols().map { DebugSymbolRenderer.render(it) }
                val classifierNames = scope.getPossibleClassifierNames()
                val renderedClassifiers = scope.getClassifierSymbols().map { DebugSymbolRenderer.render(it) }

                "FILE SYMBOL:\n" + renderedSymbol +
                        "\nCALLABLE NAMES:\n" + callableNames.joinToString(prefix = "[", postfix = "]\n", separator = ", ") +
                        "\nCALLABLE SYMBOLS:\n" + renderedCallables.joinToString(separator = "\n") +
                        "\nCLASSIFIER NAMES:\n" + classifierNames.joinToString(prefix = "[", postfix = "]\n", separator = ", ") +
                        "\nCLASSIFIER SYMBOLS:\n" + renderedClassifiers.joinToString(separator = "\n")
            }
        }

        testServices.assertions.assertEqualsToFile(testDataFileSibling(".txt"), actual)
    }
}