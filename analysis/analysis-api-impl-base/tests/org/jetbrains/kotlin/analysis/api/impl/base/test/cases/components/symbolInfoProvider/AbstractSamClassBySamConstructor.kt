/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolInfoProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSamClassBySamConstructor : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = executeOnPooledThreadInReadAction {
            val symbolRenderer = DebugSymbolRenderer(renderExtra = true, renderTypeByProperties = true, renderExpandedTypes = true)
            analyseForTest(mainFile) {
                val samConstructorSymbol = getSamConstructorSymbol(mainFile, testServices)
                val constructedClass = samConstructorSymbol?.constructedClass

                buildString {
                    appendLine("CONSTRUCTOR:")
                    appendLine("  ${samConstructorSymbol?.let { symbolRenderer.render(this@analyseForTest, it) }}")
                    appendLine("CLASS:")
                    appendLine("  ${constructedClass?.let { symbolRenderer.render(this@analyseForTest, it) }}")
                }
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private fun KaSession.getSamConstructorSymbol(mainFile: KtFile, testServices: TestServices): KaSamConstructorSymbol? {
        val callExpression = testServices.expressionMarkerProvider.getElementOfTypeAtCaretOrNull<KtCallExpression>(mainFile)
        val constructorSymbol = callExpression?.calleeExpression?.resolveToCall()
            ?.singleFunctionCallOrNull()?.partiallyAppliedSymbol?.symbol as? KaSamConstructorSymbol
        return constructorSymbol
    }
}