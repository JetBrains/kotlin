/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations

import org.jetbrains.kotlin.analysis.api.symbols.KaDebugRenderer
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.indented
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractAnalysisApiAnnotationsOnTypesTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val ktTypeReference = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtTypeReference>(mainFile)
        val actual = copyAwareAnalyzeForTest(ktTypeReference) { contextTypeReference ->
            val ktType = contextTypeReference.type
            val annotations = ktType.annotations
            buildString {
                appendLine("${KtTypeReference::class.simpleName}: ${contextTypeReference.text}")
                appendLine("annotations: [")
                for (annotation in annotations) {
                    appendLine(KaDebugRenderer().renderAnnotationApplication(useSiteSession, annotation).indented(indent = 2))
                }
                appendLine("]")
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
