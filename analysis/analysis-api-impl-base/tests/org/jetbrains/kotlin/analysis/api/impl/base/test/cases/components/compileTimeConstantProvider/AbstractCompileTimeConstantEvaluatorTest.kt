/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compileTimeConstantProvider

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.renderFrontendIndependentKClassNameOf
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractCompileTimeConstantEvaluatorTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val element = testServices.expressionMarkerProvider.getBottommostSelectedElementOfTypeByDirective(mainFile, mainModule)
        val expression = when (element) {
            is KtExpression -> element
            is KtValueArgument -> element.getArgumentExpression()
            else -> null
        } ?: testServices.assertions.fail { "Unsupported expression: $element" }
        val constantValue = executeOnPooledThreadInReadAction {
            analyseForTest(expression) {
                expression.evaluate()
            }
        }

        val actual = buildString {
            appendLine("expression: ${expression.text}")
            appendLine("constant: ${constantValue?.render() ?: "NOT_EVALUATED"}")

            appendLine("constantValueKind: ${constantValue?.let { renderFrontendIndependentKClassNameOf(it) } ?: "NOT_EVALUATED"}")
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
