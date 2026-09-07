/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolInfoProvider

import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.isFullValueClass
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.targets.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractIsFullValueClassTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val target: KtElement = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaretOrNull<KtClassOrObject>(mainFile)
            ?: mainFile

        val actual = copyAwareAnalyzeForTest(target) { contextElement ->
            val classSymbol = when (contextElement) {
                is KtClassOrObject -> contextElement.symbol as KaNamedClassSymbol
                is KtFile -> getSingleTestTargetSymbolOfType<KaNamedClassSymbol>(testDataPath, contextElement)
                else -> error("Unexpected test target: ${contextElement::class.simpleName}")
            }
            buildString {
                appendLine("CLASS:")
                appendLine("  ${classSymbol.classId ?: classSymbol.name}")
                appendLine("IS_FULL_VALUE_CLASS:")
                appendLine("  ${classSymbol.isFullValueClass}")
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
