/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations

import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.indented
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractAnalysisApiAnnotationsOnTypesTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val ktTypeReference = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtTypeReference>(mainFile)
        val actual = analyseForTest(ktTypeReference) {
            val ktType = ktTypeReference.getKtType()
            val annotations = ktType.annotations
            buildString {
                appendLine("KtTypeReference: ${ktTypeReference.text}")
                appendLine("annotations: [")
                for (annotation in annotations) {
                    appendLine(DebugSymbolRenderer().renderAnnotationApplication(annotation).indented(indent = 2))
                }
                appendLine("]")
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
