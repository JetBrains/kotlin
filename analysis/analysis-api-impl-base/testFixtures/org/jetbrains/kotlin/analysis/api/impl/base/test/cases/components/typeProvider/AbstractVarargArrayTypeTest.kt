/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeProvider

import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractVarargArrayTypeTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val declarationAtCaret =
                testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaretOrNull<KtParameter>(contextFile)
                    ?: error("No parameter found under the caret")
            val symbol = declarationAtCaret.symbol

            assert(symbol is KaValueParameterSymbol) {
                "Expected value parameter, got ${symbol::class} instead"
            }

            val varargArrayType = (symbol as KaValueParameterSymbol).varargArrayType
            val actual = varargArrayType?.let { type ->
                DebugSymbolRenderer().renderType(this@copyAwareAnalyzeForTest, type)
            } ?: "NOT_VARARG"

            testServices.assertions.assertEqualsToTestOutputFile(actual)
        }
    }
}
