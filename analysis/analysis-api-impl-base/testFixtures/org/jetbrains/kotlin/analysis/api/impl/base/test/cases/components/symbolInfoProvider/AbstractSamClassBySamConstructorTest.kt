/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolInfoProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.resolveToCall
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDebugRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSamClassBySamConstructorTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = executeOnPooledThreadInReadAction {
            val symbolRenderer = KaDebugRenderer(renderExtra = true, renderTypeByProperties = true, renderExpandedTypes = true)
            copyAwareAnalyzeForTest(mainFile) { contextFile ->
                val samConstructorSymbol = getSamConstructorSymbol(contextFile, testServices)
                val constructedClass = samConstructorSymbol?.constructedClass

                buildString {
                    appendLine("CONSTRUCTOR:")
                    appendLine("  ${samConstructorSymbol?.let { symbolRenderer.render(this@copyAwareAnalyzeForTest, it) }}")
                    appendLine("CLASS:")
                    appendLine("  ${constructedClass?.let { symbolRenderer.render(this@copyAwareAnalyzeForTest, it) }}")
                }
            }
        }
        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    context(_: KaSession)
    private fun getSamConstructorSymbol(mainFile: KtFile, testServices: TestServices): KaSamConstructorSymbol? {
        val callExpression = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaretOrNull<KtCallExpression>(mainFile)
        val constructorSymbol = callExpression
            ?.calleeExpression
            ?.resolveToCall()
            ?.singleFunctionCallOrNull()
            ?.symbol as? KaSamConstructorSymbol

        return constructorSymbol
    }
}
