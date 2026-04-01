/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider

import org.jetbrains.kotlin.analysis.api.symbols.KaDebugRenderer
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractExpectedExpressionTypeTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val expression = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret(mainFile) as KtExpression

        val actualExpectedTypeText = executeOnPooledThreadInReadAction {
            copyAwareAnalyzeForTest(expression) { contextExpression ->
                val expectedType = contextExpression.expectedType ?: return@copyAwareAnalyzeForTest null
                KaDebugRenderer().renderType(useSiteSession, expectedType)
            }
        }

        val actual = buildString {
            appendLine("expression: ${expression.text}")
            appendLine("expected type: $actualExpectedTypeText")
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
