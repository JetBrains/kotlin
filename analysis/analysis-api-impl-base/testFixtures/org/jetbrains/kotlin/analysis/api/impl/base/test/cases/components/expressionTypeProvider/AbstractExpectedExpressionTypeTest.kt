/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider

import org.jetbrains.kotlin.analysis.api.expressions.expectedType
import org.jetbrains.kotlin.analysis.api.expressions.inferredExpectedType
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.analysis.api.symbols.KaDebugRenderer
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractExpectedExpressionTypeTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val expression = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret(mainFile) as KtExpression

        val [actualExpectedTypeText, actualInferredExpectedTypeText] = executeOnPooledThreadInReadAction {
            copyAwareAnalyzeForTest(expression) { contextExpression ->
                val debugRenderer = KaDebugRenderer()
                val expectedTypeText = contextExpression.expectedType?.let { debugRenderer.renderType(useSiteSession, it) }
                val inferredExpectedTypeText = contextExpression.inferredExpectedType?.let { debugRenderer.renderType(useSiteSession, it) }
                expectedTypeText to inferredExpectedTypeText
            }
        }

        val actual = prettyPrint {
            appendLine("expression:")
            withIndent { appendLine(expression.text) }
            appendLine()
            appendLine("expectedType:")
            withIndent { appendLine(actualExpectedTypeText) }
            appendLine()
            appendLine("inferredExpectedType:")
            withIndent { appendLine(actualInferredExpectedTypeText) }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
