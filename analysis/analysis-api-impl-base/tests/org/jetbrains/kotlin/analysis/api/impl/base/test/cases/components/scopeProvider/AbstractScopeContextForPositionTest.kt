/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.KtScopeContext
import org.jetbrains.kotlin.analysis.api.components.KtScopeKind
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider.TestScopeRenderer.renderForTests
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedSingleModuleTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractScopeContextForPositionTest : AbstractAnalysisApiBasedSingleModuleTest() {
    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val ktFile = ktFiles.first()
        val element = testServices.expressionMarkerProvider.getSelectedElementOfType<KtElement>(ktFile)

        analyseForTest(element) { elementToAnalyze ->
            val scopeContext = ktFile.getScopeContextForPosition(elementToAnalyze)

            val scopeContextStringRepresentation = renderForTests(elementToAnalyze, scopeContext)
            val scopeContextStringRepresentationPretty = renderForTests(elementToAnalyze, scopeContext, printPretty = true)

            testServices.assertions.assertEqualsToTestDataFileSibling(scopeContextStringRepresentation)
            testServices.assertions.assertEqualsToTestDataFileSibling(scopeContextStringRepresentationPretty, extension = ".pretty.txt")
        }
    }

    private fun KtAnalysisSession.renderForTests(
        element: KtElement,
        scopeContext: KtScopeContext,
        printPretty: Boolean = false
    ): String = prettyPrint {
        appendLine("element: ${element.text}")
        renderForTests(scopeContext, printPretty) { scopeKind ->
            scopeKind !is KtScopeKind.DefaultSimpleImportingScope && scopeKind !is KtScopeKind.DefaultStarImportingScope
        }
    }
}
