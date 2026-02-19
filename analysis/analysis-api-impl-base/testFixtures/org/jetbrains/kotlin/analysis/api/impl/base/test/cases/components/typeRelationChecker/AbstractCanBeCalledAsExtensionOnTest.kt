/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeRelationChecker

import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractCanBeCalledAsExtensionOnTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val callable = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtCallableDeclaration>(mainFile)
        val expression = testServices.expressionMarkerProvider.getTopmostSelectedElementOfType<KtExpression>(mainFile)
        val actual = copyAwareAnalyzeForTest(mainFile) { _ ->
            val callableSymbol = callable.symbol as KaCallableSymbol
            val expressionType = expression.expressionType ?: error("Expression type should not be null for ${expression.text}")

            val canBeCalled = callableSymbol.canBeCalledAsExtensionOn(expressionType)

            buildString {
                appendLine("CAN_BE_CALLED_AS_EXTENSION_ON: $canBeCalled")
                appendLine("CALLABLE: ${callableSymbol.render()}")
                appendLine("TYPE: ${expressionType.render(position = Variance.INVARIANT)}")
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}