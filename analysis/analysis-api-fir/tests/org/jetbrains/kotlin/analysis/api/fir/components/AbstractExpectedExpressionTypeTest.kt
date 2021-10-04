/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.fir.test.framework.AbstractHLApiSingleFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractExpectedExpressionTypeTest : AbstractHLApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val expressionAtCaret = testServices.expressionMarkerProvider.getElementOfTypAtCaret(ktFile) as KtExpression

        val actualExpectedTypeText: String? = executeOnPooledThreadInReadAction {
            analyseForTest(expressionAtCaret) {
                expressionAtCaret.getExpectedType()?.asStringForDebugging()
            }
        }

        val actual = buildString {
            appendLine("expression: ${expressionAtCaret.text}")
            appendLine("expected type: $actualExpectedTypeText")
        }

        testServices.assertions.assertEqualsToFile(testDataFileSibling(".txt"), actual)
    }
}
