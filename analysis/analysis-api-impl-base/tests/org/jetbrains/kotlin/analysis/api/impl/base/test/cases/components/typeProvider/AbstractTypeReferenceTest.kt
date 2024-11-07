/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeProvider

import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractTypeReferenceTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val expressionAtCaret = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret(mainFile) as KtTypeReference

        val actual = analyseForTest(expressionAtCaret) {
            val kaType = expressionAtCaret.type
            buildString {
                appendLine("${KtTypeReference::class.simpleName}: ${expressionAtCaret.text}")
                val renderedType = if (kaType is KaErrorType) {
                    kaType.presentableText + ": " + kaType.errorMessage
                } else {
                    kaType.render(position = Variance.INVARIANT)
                }
                appendLine("${KaType::class.simpleName}: $renderedType")
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
