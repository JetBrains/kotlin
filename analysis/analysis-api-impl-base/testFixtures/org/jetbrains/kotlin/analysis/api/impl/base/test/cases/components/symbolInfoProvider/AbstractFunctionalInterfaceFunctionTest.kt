/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolInfoProvider

import org.jetbrains.kotlin.analysis.api.symbols.KaDebugRenderer
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractFunctionalInterfaceFunctionTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val typeReference = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtTypeReference>(mainFile)
        val actual = executeOnPooledThreadInReadAction {
            val symbolRenderer = KaDebugRenderer(renderExtra = true, renderTypeByProperties = true, renderExpandedTypes = true)
            copyAwareAnalyzeForTest(typeReference) { contextTypeReference ->
                val classSymbol = contextTypeReference.type.symbol ?: error("Cannot resolve type reference to a class-like symbol")
                val functionalInterfaceFunction = classSymbol.functionalInterfaceFunction
                val samConstructor = classSymbol.samConstructor
                val functionalInterfaceFunctionFromConstructor = samConstructor?.functionalInterfaceFunction

                testServices.assertions.assertEquals(functionalInterfaceFunction, functionalInterfaceFunctionFromConstructor) {
                    "Functional interface functions from class and from SAM constructor should match"
                }

                buildString {
                    appendLine("CLASS:")
                    appendLine("  ${symbolRenderer.render(this@copyAwareAnalyzeForTest, classSymbol)}")
                    appendLine()
                    appendLine("FUNCTIONAL INTERFACE FUNCTION:")
                    appendLine("  ${functionalInterfaceFunction?.let { symbolRenderer.render(this@copyAwareAnalyzeForTest, it) }}")
                }
            }
        }
        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
