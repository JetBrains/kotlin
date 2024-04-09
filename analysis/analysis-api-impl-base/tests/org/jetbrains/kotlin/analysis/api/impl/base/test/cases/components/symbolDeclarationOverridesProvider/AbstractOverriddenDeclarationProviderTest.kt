/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationOverridesProvider

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KtTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
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
                val allOverriddenSymbols = symbol.getAllOverriddenSymbols().map { renderSignature(it) }
                val directlyOverriddenSymbols = symbol.getDirectlyOverriddenSymbols().map { renderSignature(it) }
                buildString {
                    appendLine("ALL:")
                    allOverriddenSymbols.forEach { appendLine("  $it") }
                    appendLine("DIRECT:")
                    directlyOverriddenSymbols.forEach { appendLine("  $it") }
                }
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private fun KtAnalysisSession.getCallableSymbol(mainFile: KtFile, testServices: TestServices): KtCallableSymbol {
        val declaration = testServices.expressionMarkerProvider.getElementOfTypeAtCaretOrNull<KtDeclaration>(mainFile)
        if (declaration != null) {
            return declaration.getSymbol() as KtCallableSymbol
        }
        return getSingleTestTargetSymbolOfType<KtCallableSymbol>(mainFile, testDataPath)
    }

    private fun KtAnalysisSession.renderSignature(symbol: KtCallableSymbol): String = buildString {
        append(renderDeclarationQualifiedName(symbol))
        if (symbol is KtFunctionSymbol) {
            append("(")
            symbol.valueParameters.forEachIndexed { index, parameter ->
                append(parameter.name.identifier)
                append(": ")
                append(parameter.returnType.render(KtTypeRendererForSource.WITH_SHORT_NAMES, position = Variance.INVARIANT))
                if (index != symbol.valueParameters.lastIndex) {
                    append(", ")
                }
            }
            append(")")
        }
        append(": ")
        append(symbol.returnType.render(KtTypeRendererForSource.WITH_SHORT_NAMES, position = Variance.INVARIANT))
    }

    private fun KtAnalysisSession.renderDeclarationQualifiedName(symbol: KtCallableSymbol): String {
        val parentsWithSelf = generateSequence<KtSymbol>(symbol) { it.getContainingSymbol() }
            .toList()
            .asReversed()

        val chunks = mutableListOf<String>()

        for ((index, parent) in parentsWithSelf.withIndex()) {
            // Render qualified names for top-level declarations
            if (index == 0) {
                val qualifiedName = when (parent) {
                    is KtClassLikeSymbol -> parent.classIdIfNonLocal?.toString()
                    is KtCallableSymbol -> parent.callableIdIfNonLocal?.toString()
                    else -> null
                }

                if (qualifiedName != null) {
                    chunks += qualifiedName
                    continue
                }
            }

            chunks += (parent as? KtNamedSymbol)?.name?.asString() ?: "<no name>"
        }

        return chunks.joinToString(".")
    }
}