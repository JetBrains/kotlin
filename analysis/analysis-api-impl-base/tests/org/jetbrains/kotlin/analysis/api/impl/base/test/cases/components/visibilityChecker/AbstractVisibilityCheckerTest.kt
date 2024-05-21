/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.visibilityChecker

import org.jetbrains.kotlin.analysis.api.impl.base.test.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithVisibility
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * To find the element for the use-site position, the visibility checker test looks for an element called "useSite" in the main module if
 * the main file doesn't or cannot contain a caret marker, e.g. in files from binary libraries. The target name is case-insensitive, so
 * classes called `UseSite` will be found as well.
 */
private const val USE_SITE_ELEMENT_NAME = "usesite"

/**
 * Checks whether a declaration is visible from a specific use-site file and element.
 *
 * The declaration symbol is found via a symbol name at the bottom of the test file, such as `// class: Declaration` (see [SymbolByFqName]).
 */
abstract class AbstractVisibilityCheckerTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actualText = analyseForTest(mainFile) {
            val declarationSymbol = getSingleTestTargetSymbolOfType<KaSymbolWithVisibility>(mainFile, testDataPath)

            val useSiteElement = testServices.expressionMarkerProvider.getElementOfTypeAtCaretOrNull<KtExpression>(mainFile)
                ?: findFirstUseSiteElement(mainFile)
                ?: error("Cannot find use-site element to check visibility at.")

            val useSiteFileSymbol = mainFile.getFileSymbol()

            val visible = isVisible(declarationSymbol, useSiteFileSymbol, null, useSiteElement)
            """
                Declaration: ${(declarationSymbol as KaDeclarationSymbol).render(KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES)}
                At usage site: ${useSiteElement.text}
                Is visible: $visible
            """.trimIndent()
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actualText)
    }

    private fun findFirstUseSiteElement(ktFile: KtFile): KtNamedDeclaration? =
        ktFile.findDescendantOfType<KtNamedDeclaration> { it.name?.lowercase() == USE_SITE_ELEMENT_NAME }
}
