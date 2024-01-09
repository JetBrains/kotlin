/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.smartCastProvider

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractHLSmartCastInfoTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val expression = testServices.expressionMarkerProvider.getSelectedElement(mainFile) as KtExpression
        val actual = executeOnPooledThreadInReadAction {
            analyseForTest(expression) {
                val smartCastInfo = expression.getSmartCastInfo()
                buildString {
                    appendLine("expression: ${expression.text}")
                    appendLine("isStable: ${smartCastInfo?.isStable}")
                    appendLine("smartCastType: ${smartCastInfo?.smartCastType?.render(position = Variance.INVARIANT)}")

                    val receiverSmartCasts = expression.getImplicitReceiverSmartCast()
                    for (receiverSmartCast in receiverSmartCasts) {
                        appendLine("receiver: ${receiverSmartCast.kind}")
                        appendLine("    smartCastType: ${receiverSmartCast.type.render(position = Variance.INVARIANT)}")
                    }
                }
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}