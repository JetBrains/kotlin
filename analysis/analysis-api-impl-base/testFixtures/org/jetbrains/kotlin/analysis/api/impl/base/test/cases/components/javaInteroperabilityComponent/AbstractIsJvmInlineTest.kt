/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.javaInterop.isJvmInline
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

@OptIn(KaExperimentalApi::class)
abstract class AbstractIsJvmInlineTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actualText = executeOnPooledThreadInReadAction {
            copyAwareAnalyzeForTest(mainFile) { contextFile ->
                val ktClass = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtClassOrObject>(contextFile)
                val symbol = ktClass.symbol as KaNamedClassSymbol

                buildString {
                    appendLine("isJvmInline:")
                    appendLine(symbol.isJvmInline)
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actualText)
    }
}
