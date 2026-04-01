/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.relationProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.resolveToSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDebugRenderer
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractOriginalConstructorIfTypeAliasedTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = executeOnPooledThreadInReadAction {
            val symbolRenderer = KaDebugRenderer(renderExtra = true, renderExpandedTypes = true)
            copyAwareAnalyzeForTest(mainFile) { contextFile ->
                val referencedConstructor = getReferencedConstructorSymbol(contextFile, testServices) ?: error("No constructor symbol")
                val originalConstructor = referencedConstructor.originalConstructorIfTypeAliased

                prettyPrint {
                    appendLine("Resolved constructor:")
                    withIndent {
                        appendLine(symbolRenderer.render(this@copyAwareAnalyzeForTest, referencedConstructor))
                    }
                    appendLine()
                    appendLine("Original constructor if type aliased:")
                    withIndent {
                        append("")
                        appendLine(originalConstructor?.let { symbolRenderer.render(this@copyAwareAnalyzeForTest, it) }.toString())
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    context(_: KaSession)
    private fun getReferencedConstructorSymbol(mainFile: KtFile, testServices: TestServices): KaConstructorSymbol? {
        val reference = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaretOrNull<KtSimpleNameExpression>(mainFile)

        return reference?.mainReference?.resolveToSymbol() as? KaConstructorSymbol
    }
}
