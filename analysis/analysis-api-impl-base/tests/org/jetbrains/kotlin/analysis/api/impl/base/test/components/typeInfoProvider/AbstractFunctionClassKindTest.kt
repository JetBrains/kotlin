/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.components.typeInfoProvider

import org.jetbrains.kotlin.analysis.api.impl.barebone.test.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.api.impl.base.test.test.framework.AbstractHLApiSingleFileTest
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractFunctionClassKindTest (
    configurator: FrontendApiTestConfiguratorService
) : AbstractHLApiSingleFileTest(configurator) {

    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val expressionAtCaret = testServices.expressionMarkerProvider.getElementOfTypAtCaret(ktFile) as KtExpression

        val (type, functionClassKind) = executeOnPooledThreadInReadAction {
            analyseForTest(expressionAtCaret) {
                val functionType = expressionAtCaret.getExpectedType()
                functionType?.render() to functionType?.functionClassKind
            }
        }
        val actual = buildString {
            appendLine("expression: ${expressionAtCaret.text}")
            appendLine("expected type: $type")
            appendLine("functionClassKind: $functionClassKind")
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
