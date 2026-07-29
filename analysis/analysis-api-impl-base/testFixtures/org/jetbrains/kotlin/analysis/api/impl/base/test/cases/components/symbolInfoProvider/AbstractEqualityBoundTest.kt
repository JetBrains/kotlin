/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolInfoProvider

import org.jetbrains.kotlin.analysis.api.renderer.render
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.equalityBound
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractEqualityBoundTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val function = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaretOrNull<KtNamedFunction>(mainFile)
            ?: error("No function at caret found. Please add a caret to a function declaration.")

        val actual = copyAwareAnalyzeForTest(function) { contextFunction ->
            val functionSymbol = contextFunction.symbol as? KaNamedFunctionSymbol ?: error("No named function symbol found at the caret.")
            val equalityBound: KaType? = functionSymbol.equalityBound

            buildString {
                appendLine("FUNCTION:")
                appendLine("  ${functionSymbol.callableId ?: functionSymbol.name}")
                appendLine("EQUALITY_BOUND:")
                appendLine("  ${equalityBound?.render(position = Variance.INVARIANT) ?: "null"}")
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
