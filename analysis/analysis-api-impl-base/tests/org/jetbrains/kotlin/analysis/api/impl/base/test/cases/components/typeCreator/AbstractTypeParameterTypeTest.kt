/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeCreator

import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractTypeParameterTypeTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val expressionAtCaret = testServices.expressionMarkerProvider.getElementOfTypeAtCaret(mainFile) as KtTypeParameter

        val actual = analyseForTest(expressionAtCaret) {
            val symbol = expressionAtCaret.symbol
            val ktType = buildTypeParameterType(symbol)
            buildString {
                appendLine("${KtTypeParameter::class.simpleName}: ${expressionAtCaret.text}")
                appendLine("${KaType::class.simpleName}: ${ktType.render(position = Variance.INVARIANT)}")
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
