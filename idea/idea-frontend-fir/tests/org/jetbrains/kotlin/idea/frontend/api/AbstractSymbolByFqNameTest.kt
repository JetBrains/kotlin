/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import org.jetbrains.kotlin.idea.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.frontend.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.nio.file.Paths

abstract class AbstractSymbolByFqNameTest : KotlinLightCodeInsightFixtureTestCase() {
    protected fun doTest(path: String) {
        val fakeKtFile = myFixture.configureByText("file.kt", "fun a() {}") as KtFile

        val symbolData = SymbolByFqName.getSymbolDataFromFile(Paths.get(path))

        val renderedSymbols = executeOnPooledThreadInReadAction {
            analyse(fakeKtFile) {
                val symbols = createSymbols(symbolData)
                symbols.map { DebugSymbolRenderer.render(it) }
            }
        }

        val actual = SymbolByFqName.textWithRenderedSymbolData(path, renderedSymbols.joinToString(separator = "\n"))
        KotlinTestUtils.assertEqualsToFile(File(path), actual)
    }

    protected abstract fun KtAnalysisSession.createSymbols(symbolData: SymbolData): List<KtSymbol>

    override fun getDefaultProjectDescriptor(): KotlinLightProjectDescriptor =
        KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}

