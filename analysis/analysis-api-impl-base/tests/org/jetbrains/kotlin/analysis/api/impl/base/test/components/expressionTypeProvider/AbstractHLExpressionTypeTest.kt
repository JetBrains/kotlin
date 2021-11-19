/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.components.expressionTypeProvider

import org.jetbrains.kotlin.analysis.api.impl.barebone.test.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.api.impl.base.test.test.framework.AbstractHLApiSingleFileTest
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractHLExpressionTypeTest(configurator: FrontendApiTestConfiguratorService) : AbstractHLApiSingleFileTest(configurator) {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val expression = testServices.expressionMarkerProvider.getSelectedElement(ktFile) as KtExpression
        val type = executeOnPooledThreadInReadAction {
            analyseForTest(expression) { expression.getKtType()?.render() }
        }
        val actual = buildString {
            appendLine("expression: ${expression.text}")
            appendLine("type: $type")
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}