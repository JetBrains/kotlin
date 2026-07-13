/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolInfoProvider

import org.jetbrains.kotlin.analysis.api.utils.getApiKClassOf
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractDefaultAnnotationTargetsTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val declaration = testServices.expressionMarkerProvider.getBottommostSelectedElementOfTypeOrNull<KtDeclaration>(mainFile)

        val actual = copyAwareAnalyzeForTest(declaration ?: mainFile) { contextElement ->
            val symbol = when (contextElement) {
                is KtFile -> contextElement.symbol
                is KtDeclaration -> contextElement.symbol
                else -> error("Unexpected context element $contextElement")
            }

            val defaultAnnotationTargets = symbol.defaultAnnotationTargets.orEmpty().toList().sortedBy { it.name }

            buildString {
                appendLine("Symbol:")
                append("  ").appendLine(getApiKClassOf(symbol).simpleName)
                appendLine("Default annotation targets:")
                defaultAnnotationTargets.forEach { appendLine("  $it") }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
