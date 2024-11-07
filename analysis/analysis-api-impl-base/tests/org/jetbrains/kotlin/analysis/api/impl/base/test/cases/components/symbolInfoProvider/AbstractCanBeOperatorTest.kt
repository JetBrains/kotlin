/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolInfoProvider

import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractCanBeOperatorTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val function = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaretOrNull<KtNamedFunction>(mainFile)
        val symbolRenderer = DebugSymbolRenderer(renderExtra = true, renderTypeByProperties = true, renderExpandedTypes = true)
        val actual = analyseForTest(mainFile) {
            val functionSymbol = function?.symbol as? KaNamedFunctionSymbol ?: error("NO NAMED FUNCTION UNDER CARET")
            val canBeOperator = functionSymbol.canBeOperator

            buildString {
                appendLine("FUNCTION:")
                appendLine("  ${symbolRenderer.render(this@analyseForTest, functionSymbol)}")
                appendLine("CAN_BE_OPERATOR:")
                appendLine("  $canBeOperator")
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}