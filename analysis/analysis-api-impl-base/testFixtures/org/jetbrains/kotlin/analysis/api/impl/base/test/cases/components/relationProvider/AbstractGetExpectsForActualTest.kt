/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.relationProvider

import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractGetExpectsForActualTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val renderer = KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES
        val declaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtDeclaration>(mainFile)
        val expectedSymbolText = executeOnPooledThreadInReadAction {
            copyAwareAnalyzeForTest(declaration) { contextDeclaration ->
                val symbol = contextDeclaration.symbol
                val expectedSymbols = if (symbol is KaCallableSymbol) {
                    // For callable symbols, exercise the endpoint also for their receiver parameter,
                    // since it is kind of a special case and not a `KtDeclaration` itself.
                    symbol.receiverParameter?.getExpectsForActual().orEmpty() + symbol.getExpectsForActual()
                } else {
                    symbol.getExpectsForActual()
                }

                expectedSymbols.joinToString(separator = "\n") { expectedSymbol ->
                    val prefix = if (expectedSymbol is KaReceiverParameterSymbol) "receiver parameter " else ""
                    expectedSymbol.psi?.containingFile?.name + " : " + prefix + expectedSymbol.render(renderer)
                }
            }
        }

        val actual = buildString {
            appendLine("expected symbols:")
            appendLine(expectedSymbolText)
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}