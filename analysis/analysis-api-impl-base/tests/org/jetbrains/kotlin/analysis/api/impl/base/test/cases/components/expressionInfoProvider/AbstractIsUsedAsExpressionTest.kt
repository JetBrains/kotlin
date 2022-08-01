/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionInfoProvider

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractIsUsedAsExpressionTest : AbstractAnalysisApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {

        val expression = testServices.expressionMarkerProvider.getSelectedElementOfType<KtExpression>(ktFile).let {
            if (it is KtBlockExpression && it.statements.size == 1 && it.textRange == it.statements.single().textRange) {
                it.statements.single()
            } else {
                it
            }
        }
        val actual = StringBuilder();

        analyseForTest(expression) {
            actual.appendLine("expression: $expression")
            actual.appendLine("text: ${expression.text}")
            actual.appendLine("isUsedAsExpression: ${expression.isUsedAsExpression()}")
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual.toString())
    }
}