/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider

import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForDebug
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractHLExpressionTypeTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val selected = testServices.expressionMarkerProvider.getTopmostSelectedElementOfTypeByDirective(mainFile, mainModule).let {
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

        val caretElement = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaretOrNull<KtElement>(mainFile)

        val type = executeOnPooledThreadInReadAction {
            copyAwareAnalyzeForTest(caretElement ?: expression) {
                var ktType = expression.expressionType
                if (Directives.APPROXIMATE_TYPE in mainModule.testModule.directives) {
                    ktType = if (caretElement == null) {
                        ktType?.approximateToDenotableSupertypeOrSelf(allowLocalDenotableTypes = true)
                    } else {
                        ktType?.approximateToDenotableSupertypeOrSelf(caretElement)
                    }
                }
                ktType?.render(renderer, position = Variance.INVARIANT)
            }
        }

        val actual = buildString {
            appendLine("expression: ${expression.text}")
            appendLine("type: $type")
        }
        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    object Directives : SimpleDirectivesContainer() {
        val APPROXIMATE_TYPE by stringDirective("approximate expression type")
    }

    companion object {
        private val renderer = KaTypeRendererForDebug.WITH_QUALIFIED_NAMES
    }
}
