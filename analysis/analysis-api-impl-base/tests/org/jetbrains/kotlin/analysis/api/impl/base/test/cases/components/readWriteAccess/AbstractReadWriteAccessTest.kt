/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.readWriteAccess

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractReadWriteAccessTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val expressionAtCaret = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtExpression>(mainFile)

        val readWriteAccess = expressionAtCaret.readWriteAccess(true)

        val actual = buildString {
            appendLine("expression: ${expressionAtCaret.text}")
            appendLine("access type: $readWriteAccess")
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
