/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.components.expressionTypeProvider

import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractExpectedExpressionTypeTest : AbstractAnalysisApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val expressionAtCaret = testServices.expressionMarkerProvider.getElementOfTypAtCaret(ktFile) as KtExpression

        val actualExpectedTypeText: String? = executeOnPooledThreadInReadAction {
            analyseForTest(expressionAtCaret) {
                val expectedType = expressionAtCaret.getExpectedType() ?: return@analyseForTest null
                DebugSymbolRenderer.renderType(expectedType)
            }
        }

        val actual = buildString {
            appendLine("expression: ${expressionAtCaret.text}")
            appendLine("expected type: $actualExpectedTypeText")
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
