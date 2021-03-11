/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.idea.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.idea.test.framework.AbstractKtIdeaTest
import org.jetbrains.kotlin.idea.test.framework.TestFileStructure
import org.jetbrains.kotlin.idea.test.framework.TestStructureExpectedDataBlock
import org.jetbrains.kotlin.idea.test.framework.TestStructureRenderer
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.test.KotlinTestUtils

abstract class AbstractOverriddenDeclarationProviderTest : AbstractKtIdeaTest() {
    override fun doTestByFileStructure(fileStructure: TestFileStructure) {
        val signatures = executeOnPooledThreadInReadAction {
            analyze(fileStructure.mainKtFile) {
                val symbol = getElementOfTypeAtCaret<KtDeclaration>().getSymbol() as KtCallableSymbol
                val allOverriddenSymbols = symbol.getAllOverriddenSymbols().map { renderSignature(it) }
                val directlyOverriddenSymbols = symbol.getDirectlyOverriddenSymbols().map { renderSignature(it) }
                listOf(
                    TestStructureExpectedDataBlock("ALL:", allOverriddenSymbols),
                    TestStructureExpectedDataBlock("DIRECT:", directlyOverriddenSymbols),
                )
            }
        }
        val actual = TestStructureRenderer.render(fileStructure, signatures)
        KotlinTestUtils.assertEqualsToFile(fileStructure.filePath.toFile(), actual)
    }

    private fun KtAnalysisSession.renderSignature(symbol: KtCallableSymbol): String = buildString {
        append(getPath(symbol))
        if (symbol is KtFunctionSymbol) {
            append("(")
            symbol.valueParameters.forEachIndexed { index, parameter ->
                append(parameter.name.identifier)
                append(": ")
                append(parameter.annotatedType.type.render(KtTypeRendererOptions.SHORT_NAMES))
                if (index != symbol.valueParameters.lastIndex) {
                    append(", ")
                }
            }
            append(")")
        }
        append(": ")
        append(symbol.annotatedType.type.render(KtTypeRendererOptions.SHORT_NAMES))
    }

    private fun getPath(symbol: KtCallableSymbol): String = when (symbol) {
        is KtSyntheticJavaPropertySymbol -> symbol.callableIdIfNonLocal?.asString()!!
        else -> {
            val ktDeclaration = symbol.psi as KtDeclaration
            ktDeclaration
                .parentsOfType<KtDeclaration>(withSelf = true)
                .map { it.name ?: "<no name>" }
                .toList()
                .asReversed()
                .joinToString(separator = ".")
        }
    }
}