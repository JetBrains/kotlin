/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.visibilityChecker

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.ExpressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.targets.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * To find the element for the use-site position, the visibility checker test looks for an element called "useSite" in the main module if
 * the main file doesn't or cannot contain a caret marker, e.g., in files from binary libraries. The target name is case-insensitive, so
 * classes called `UseSite` will be found as well.
 */
private const val USE_SITE_ELEMENT_NAME = "usesite"

/**
 * For local named declaration targets,
 * the callable id cannot be specified in the [TestSymbolTarget][org.jetbrains.kotlin.analysis.test.framework.targets.TestSymbolTarget]
 * directive format.
 * Instead, such declarations can be distinguished by the caret marker `<caret_target>`.
 */
private const val TARGET_QUALIFIER = "target"

/**
 * Checks whether a declaration is visible from a specific use-site file and element.
 *
 * The declaration symbol is found via a symbol name at the bottom of the test file, such as `// class: Declaration` (see
 * [getSingleTestTargetSymbolOfType]) or by the caret marker `<caret_target>`.
 */
abstract class AbstractVisibilityCheckerTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actualText = copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val declarationSymbol = findTargetSymbol(contextFile, testServices.expressionMarkerProvider)
            val useSiteElement = findUseSiteElement(contextFile, testServices.expressionMarkerProvider)
            val receiverExpression = useSiteElement.parentOfType<KtDotQualifiedExpression>()?.receiverExpression
            val useSiteFileSymbol = contextFile.symbol

            val resultWithoutReceiver = checkVisibilityAndBuildResult(
                declarationSymbol,
                useSiteElement,
                useSiteFileSymbol,
                receiverExpression = null,
                testServices,
            )

            if (receiverExpression == null) {
                return@copyAwareAnalyzeForTest resultWithoutReceiver
            }

            val resultWithReceiver = checkVisibilityAndBuildResult(
                declarationSymbol,
                useSiteElement,
                useSiteFileSymbol,
                receiverExpression,
                testServices,
            )
            resultWithoutReceiver + "\n\n" + resultWithReceiver
        }

        testServices.assertions.assertEqualsToTestOutputFile(actualText)
    }

    private fun KaSession.findTargetSymbol(contextFile: KtFile, provider: ExpressionMarkerProvider): KaDeclarationSymbol {
        val declarationByCaret = provider.getBottommostElementOfTypeAtCaretOrNull<KtNamedDeclaration>(contextFile, TARGET_QUALIFIER)
        return declarationByCaret?.symbol ?: getSingleTestTargetSymbolOfType<KaDeclarationSymbol>(testDataPath, contextFile)
    }

    private fun findUseSiteElement(contextFile: KtFile, provider: ExpressionMarkerProvider): KtExpression {
        val byCaret = provider.getBottommostElementOfTypeAtCaretOrNull<KtExpression>(contextFile)
        if (byCaret != null) return byCaret

        val namedDeclarations = if (contextFile.isCompiled) {
            generateSequence<List<KtElement>>(listOf(contextFile)) { elements ->
                elements.flatMap { element ->
                    when (element) {
                        is KtFile -> element.declarations
                        is KtScript -> element.declarations
                        is KtClassOrObject -> element.declarations
                        else -> emptyList()
                    }
                }.takeUnless(List<KtDeclaration>::isEmpty)
            }.flatten()
        } else {
            contextFile.collectDescendantsOfType<KtNamedDeclaration>().asSequence()
        }

        val byName = namedDeclarations.find { it is KtNamedDeclaration && it.name?.lowercase() == USE_SITE_ELEMENT_NAME }
        if (byName != null) {
            return byName as KtExpression
        }

        error("Cannot find use-site element to check visibility at.")
    }

    private fun KaSession.checkVisibilityAndBuildResult(
        declarationSymbol: KaDeclarationSymbol,
        useSiteElement: KtExpression,
        useSiteFileSymbol: KaFileSymbol,
        receiverExpression: KtExpression?,
        testServices: TestServices,
    ): String {
        val visibleByUseSiteVisibilityChecker = createUseSiteVisibilityChecker(
            useSiteFileSymbol,
            receiverExpression,
            useSiteElement,
        ).isVisible(declarationSymbol)

        @Suppress("DEPRECATION")
        val isVisibleByDeprecatedVisibilityFunction =
            isVisible(declarationSymbol, useSiteFileSymbol, receiverExpression, useSiteElement)

        testServices.assertions.assertEquals(isVisibleByDeprecatedVisibilityFunction, visibleByUseSiteVisibilityChecker) {
            val receiverDescription = if (receiverExpression != null) " with receiver" else ""
            "createUseSiteVisibilityChecker(..).isVisible(..)$receiverDescription returning $visibleByUseSiteVisibilityChecker" +
                    " is inconsistent with isVisible(...) returning $isVisibleByDeprecatedVisibilityFunction"
        }

        return """
            Declaration: ${declarationSymbol.render(KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES)}
            At usage site: ${useSiteElement.text}
            Is visible: $visibleByUseSiteVisibilityChecker
            Receiver expression: ${receiverExpression?.text}
        """.trimIndent()
    }
}
