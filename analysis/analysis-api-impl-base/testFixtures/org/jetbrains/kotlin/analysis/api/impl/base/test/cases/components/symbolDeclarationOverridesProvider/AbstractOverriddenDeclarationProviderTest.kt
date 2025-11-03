/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationOverridesProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.containingDeclaration
import org.jetbrains.kotlin.analysis.api.components.render
import org.jetbrains.kotlin.analysis.api.components.resolveToSymbol
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.targets.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractOverriddenDeclarationProviderTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = executeOnPooledThreadInReadAction {
            // Since analyzing overrides requires checking multiple declarations, we should use `PREFER_SELF` for copy-aware analysis.
            // Otherwise, supertypes might not be properly resolved.
            copyAwareAnalyzeForTest(
                mainFile,
                danglingFileResolutionMode = KaDanglingFileResolutionMode.PREFER_SELF,
            ) { contextFile ->
                val symbol = getCallableSymbol(contextFile, mainModule, testServices)

                val containingDeclaration = symbol.containingDeclaration as KaNamedClassSymbol
                val delegatedMemberScope = containingDeclaration.delegatedMemberScope
                val delegatedCallables = delegatedMemberScope.callables.toList()
                val find = delegatedCallables.find { it.name?.asString() == "clear" } ?: return@copyAwareAnalyzeForTest ""
                val directlyOverridden = find.directlyOverriddenSymbols.toList()

                val allOverriddenSymbols = /*emptyList<String>()*/symbol.allOverriddenSymbols.map { renderSignature(it) }.toList()
                val directlyOverriddenSymbols = /*emptyList<String>()*/symbol.directlyOverriddenSymbols.map { renderSignature(it) }.toList()

                // K1 doesn't support this
                val intersectionOverriddenSymbols = if (configurator.frontendKind == FrontendKind.Fe10) {
                    emptyList()
                } else {
                    symbol.intersectionOverriddenSymbols.map { renderSignature(it) }
                }

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
        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    context(_: KaSession)
    private fun getCallableSymbol(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices): KaCallableSymbol {
        val declaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaretOrNull<KtDeclaration>(mainFile)
        if (declaration != null) {
            return declaration.symbol as KaCallableSymbol
        }

        val referenceExpression = testServices.expressionMarkerProvider
            .getTopmostSelectedElementOfTypeByDirectiveOrNull(mainFile, mainModule, defaultType = KtExpression::class) as? KtExpression
        if (referenceExpression != null) {
            val reference = referenceExpression.mainReference ?: error("No reference at caret")
            val symbol = reference.resolveToSymbol() ?: error("Reference cannot be resolved")
            require(symbol is KaCallableSymbol) { "Resolved to non-callable symbol $${symbol::class.simpleName}" }
            return symbol
        }

        return getSingleTestTargetSymbolOfType<KaCallableSymbol>(testDataPath, mainFile)
    }

    context(_: KaSession)
    private fun renderSignature(symbol: KaCallableSymbol): String = buildString {
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

    context(_: KaSession)
    private fun renderDeclarationQualifiedName(symbol: KaCallableSymbol): String {
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