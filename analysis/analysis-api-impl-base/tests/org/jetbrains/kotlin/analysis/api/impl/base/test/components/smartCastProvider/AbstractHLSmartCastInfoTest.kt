/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.components.smartCastProvider

import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractHLSmartCastInfoTest : AbstractAnalysisApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val expression = testServices.expressionMarkerProvider.getSelectedElement(ktFile) as KtExpression
        val actual = executeOnPooledThreadInReadAction {
            analyseForTest(expression) {
                val smartCastInfo = expression.getSmartCastInfo()
                buildString {
                    appendLine("expression: ${expression.text}")
                    appendLine("isStable: ${smartCastInfo?.isStable}")
                    appendLine("smartCastType: ${smartCastInfo?.smartCastType?.render()}")
                }
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}