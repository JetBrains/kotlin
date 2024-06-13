/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider

import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForDebug
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractHLExpressionTypeTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val selected = testServices.expressionMarkerProvider.getSelectedElementOfTypeByDirective(mainFile, mainModule).let {
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
            analyseForTest(expression) {
                var ktType = expression.getKaType()
                if (Directives.APPROXIMATE_TYPE in mainModule.testModule.directives) {
                    ktType = ktType?.approximateToSuperPublicDenotableOrSelf(true)
                }
                ktType?.render(renderer, position = Variance.INVARIANT)
            }
        }
        val actual = buildString {
            appendLine("expression: ${expression.text}")
            appendLine("type: $type")
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    object Directives : SimpleDirectivesContainer() {
        val APPROXIMATE_TYPE by stringDirective("approximate expression type")
    }

    companion object {
        private val renderer = KaTypeRendererForDebug.WITH_QUALIFIED_NAMES
    }
}