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
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractFileImportingScopeContextTest : AbstractAnalysisApiBasedSingleModuleTest() {
    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val ktFile = ktFiles.first()
        val renderDefaultImportingScope = Directives.RENDER_DEFAULT_IMPORTING_SCOPE in module.directives

        analyseForTest(ktFile) {
            val ktScopeContext = ktFile.getImportingScopeContext()

            val scopeContextStringRepresentation = render(ktScopeContext, renderDefaultImportingScope)
            val scopeContextStringRepresentationPretty = render(ktScopeContext, renderDefaultImportingScope, printPretty = true)

            testServices.assertions.assertEqualsToTestDataFileSibling(scopeContextStringRepresentation)
            testServices.assertions.assertEqualsToTestDataFileSibling(scopeContextStringRepresentationPretty, extension = ".pretty.txt")
        }
    }

    context(KtAnalysisSession)
    private fun render(
        importingScope: KtScopeContext,
        renderDefaultImportingScope: Boolean,
        printPretty: Boolean = false
    ): String = prettyPrint {
        renderForTests(importingScope, printPretty) { ktScopeKind ->
            when (ktScopeKind) {
                is KtScopeKind.PackageMemberScope -> false
                is KtScopeKind.DefaultSimpleImportingScope -> renderDefaultImportingScope
                is KtScopeKind.DefaultStarImportingScope -> renderDefaultImportingScope
                else -> true
            }
        }
    }

    private object Directives : SimpleDirectivesContainer() {
        val RENDER_DEFAULT_IMPORTING_SCOPE by directive("render default importing scope in test output")
    }
}
