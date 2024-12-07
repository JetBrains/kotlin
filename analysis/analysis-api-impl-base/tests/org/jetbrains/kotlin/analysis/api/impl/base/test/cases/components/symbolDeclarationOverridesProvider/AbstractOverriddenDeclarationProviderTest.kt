/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationOverridesProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractOverriddenDeclarationProviderTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = executeOnPooledThreadInReadAction {
            analyseForTest(mainFile) {
                val symbol = getCallableSymbol(mainFile, testServices)
                val allOverriddenSymbols = symbol.allOverriddenSymbols.map { renderSignature(it) }
                val directlyOverriddenSymbols = symbol.directlyOverriddenSymbols.map { renderSignature(it) }

                // K1 doesn't support this
                val intersectionOverriddenSymbols = if (configurator.frontendKind == FrontendKind.Fe10)
                    emptyList()
                else
                    symbol.intersectionOverriddenSymbols.map { renderSignature(it) }

                buildString {
                    appendLine("ALL:")
                    allOverriddenSymbols.forEach { appendLine("  $it") }
                    appendLine("DIRECT:")
                    directlyOverriddenSymbols.forEach { appendLine("  $it") }
                    appendLine("INTERSECTION:")
                    intersectionOverriddenSymbols.forEach { appendLine("  $it") }
                }
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private fun KaSession.getCallableSymbol(mainFile: KtFile, testServices: TestServices): KaCallableSymbol {
        val declaration = testServices.expressionMarkerProvider.getElementOfTypeAtCaretOrNull<KtDeclaration>(mainFile)
        if (declaration != null) {
            return declaration.symbol as KaCallableSymbol
        }
        return getSingleTestTargetSymbolOfType<KaCallableSymbol>(mainFile, testDataPath)
    }

    private fun KaSession.renderSignature(symbol: KaCallableSymbol): String = buildString {
        append(renderDeclarationQualifiedName(symbol))
        if (symbol is KaNamedFunctionSymbol) {
            append("(")
            symbol.valueParameters.forEachIndexed { index, parameter ->
                append(parameter.name.identifier)
                append(": ")
                append(parameter.returnType.render(KaTypeRendererForSource.WITH_SHORT_NAMES, position = Variance.INVARIANT))
                if (index != symbol.valueParameters.lastIndex) {
                    append(", ")
                }
            }
            append(")")
        }
        append(": ")
        append(symbol.returnType.render(KaTypeRendererForSource.WITH_SHORT_NAMES, position = Variance.INVARIANT))
    }

    private fun KaSession.renderDeclarationQualifiedName(symbol: KaCallableSymbol): String {
        val parentsWithSelf = generateSequence<KaSymbol>(symbol) { it.containingDeclaration }
            .toList()
            .asReversed()

        val chunks = mutableListOf<String>()

        for ((index, parent) in parentsWithSelf.withIndex()) {
            // Render qualified names for top-level declarations
            if (index == 0) {
                val qualifiedName = when (parent) {
                    is KaClassLikeSymbol -> parent.classId?.toString()
                    is KaCallableSymbol -> parent.callableId?.toString()
                    else -> null
                }

                if (qualifiedName != null) {
                    chunks += qualifiedName
                    continue
                }
            }

            chunks += parent.name?.asString() ?: "<no name>"
        }

        return chunks.joinToString(".")
    }
}