/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.scopes

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractFileScopeTest : KotlinLightCodeInsightFixtureTestCase() {

    protected fun doTest(path: String) {
        val ktFile = myFixture.configureByText("file.kt", FileUtil.loadFile(File(path))) as KtFile

        val actual = executeOnPooledThreadInReadAction {
            analyse(ktFile) {
                val symbol = ktFile.getFileSymbol()
                val scope = symbol.getFileScope()

                val renderedSymbol = DebugSymbolRenderer.render(symbol)
                val callableNames = scope.getCallableNames()
                val renderedCallables = scope.getCallableSymbols().map { DebugSymbolRenderer.render(it) }
                val classifierNames = scope.getClassifierNames()
                val renderedClassifiers = scope.getClassifierSymbols().map { DebugSymbolRenderer.render(it) }

                "FILE SYMBOL:\n" + renderedSymbol +
                    "\nCALLABLE NAMES:\n" + callableNames.joinToString(prefix = "[", postfix = "]\n", separator = ", ") +
                    "\nCALLABLE SYMBOLS:\n" + renderedCallables.joinToString(separator = "\n") +
                    "\nCLASSIFIER NAMES:\n" + classifierNames.joinToString(prefix = "[", postfix = "]\n", separator = ", ") +
                    "\nCLASSIFIER SYMBOLS:\n" + renderedClassifiers.joinToString(separator = "\n")
            }
        }

        KotlinTestUtils.assertEqualsToFile(File("$path.result"), actual)
    }
}