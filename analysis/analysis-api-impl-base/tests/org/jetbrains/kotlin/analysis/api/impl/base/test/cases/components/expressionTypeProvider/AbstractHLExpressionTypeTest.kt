/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider

import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KtTypeRendererForDebug
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractHLExpressionTypeTest : AbstractAnalysisApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val selected = testServices.expressionMarkerProvider.getSelectedElementOfTypeByDirective(ktFile, module).let {
            if (it is KtBlockExpression && it.statements.size == 1 && it.textRange == it.statements.single().textRange) {
                it.statements.single()
            } else {
                it
            }
        }
        val expression = when (selected) {
            is KtExpression -> selected
            is KtValueArgument -> selected.getArgumentExpression()
            else -> null
        } ?: error("expect an expression but got ${selected.text}, ${selected::class}")
        val type = executeOnPooledThreadInReadAction {
            analyseForTest(expression) { expression.getKtType()?.render(renderer, position = Variance.INVARIANT) }
        }
        val actual = buildString {
            appendLine("expression: ${expression.text}")
            appendLine("type: $type")
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    companion object {
        private val renderer = KtTypeRendererForDebug.WITH_QUALIFIED_NAMES
    }
}