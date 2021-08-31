/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.frontend.api.components

import org.jetbrains.kotlin.idea.fir.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.fir.frontend.api.test.framework.AbstractHLApiSingleFileTest
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.expressionMarkerProvider
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractHLExpressionTypeTest : AbstractHLApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val expression = testServices.expressionMarkerProvider.getSelectedElement(ktFile) as KtExpression
        val type = executeOnPooledThreadInReadAction {
            analyse(expression) { expression.getKtType()?.render() }
        }
        val actual = buildString {
            appendLine("expression: ${expression.text}")
            appendLine("type: $type")
        }
        testServices.assertions.assertEqualsToFile(testDataFileSibling(".txt"), actual)
    }
}