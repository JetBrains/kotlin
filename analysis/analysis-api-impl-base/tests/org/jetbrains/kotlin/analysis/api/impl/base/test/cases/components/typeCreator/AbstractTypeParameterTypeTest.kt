/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeCreator

import org.jetbrains.kotlin.analysis.api.components.buildTypeParameterType
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractTypeParameterTypeTest : AbstractAnalysisApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val expressionAtCaret = testServices.expressionMarkerProvider.getElementOfTypeAtCaret(ktFile) as KtTypeParameter

        val actual = analyseForTest(expressionAtCaret) {
            val symbol = expressionAtCaret.getTypeParameterSymbol()
            val ktType = buildTypeParameterType(symbol)
            buildString {
                appendLine("expression: ${expressionAtCaret.text}")
                appendLine("ktType: ${ktType.render()}")
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
