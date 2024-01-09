/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolInfoProvider

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractAnnotationApplicableTargetsTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val annotationEntry = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtAnnotationEntry>(mainFile)
        val actual = analyseForTest(annotationEntry) {
            val annotationClassSymbol = annotationEntry.typeReference?.getKtType()?.expandedClassSymbol!!
            val applicableTargetsInOrder =
                annotationClassSymbol.annotationApplicableTargets
                    ?.map { it.name }
                    ?.sorted()
                    ?.joinToString(prefix = "[", separator = ", ", postfix = "]")
                    ?: "<null>"

            buildString {
                appendLine("KtAnnotationEntry: ${annotationEntry.text}")
                appendLine()
                appendLine("Resolved annotation symbol:")
                appendLine(annotationClassSymbol.render())
                appendLine()
                appendLine("Applicable targets: $applicableTargetsInOrder")
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
