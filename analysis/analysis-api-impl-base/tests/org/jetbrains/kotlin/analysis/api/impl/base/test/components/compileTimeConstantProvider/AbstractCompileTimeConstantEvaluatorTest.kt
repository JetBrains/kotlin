/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.components.compileTimeConstantProvider

import org.jetbrains.kotlin.analysis.api.impl.barebone.test.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.api.impl.base.test.test.framework.AbstractHLApiSingleFileTest
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractCompileTimeConstantEvaluatorTest(
    configurator: FrontendApiTestConfiguratorService
) : AbstractHLApiSingleFileTest(configurator) {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val element = testServices.expressionMarkerProvider.getSelectedElement(ktFile)
        val expression = when (element) {
            is KtExpression -> element
            is KtValueArgument -> element.getArgumentExpression()
            else -> null
        } ?: testServices.assertions.fail { "Unsupported expression: $element" }
        val constantValue = executeOnPooledThreadInReadAction {
            analyseForTest(expression) { expression.evaluate() }
        }
        val actual = buildString {
            appendLine("expression: ${expression.text}")
            appendLine("constant: ${constantValue?.renderAsKotlinConstant() ?: "NOT_EVALUATED"}")
            appendLine("constantValueKind: ${constantValue?.constantValueKind ?: "NOT_EVALUATED"}")
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

}