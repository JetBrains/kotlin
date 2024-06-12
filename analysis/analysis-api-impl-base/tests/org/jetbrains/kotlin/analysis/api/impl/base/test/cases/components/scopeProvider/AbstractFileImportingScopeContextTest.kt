/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaScopeContext
import org.jetbrains.kotlin.analysis.api.components.KaScopeKind
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider.TestScopeRenderer.renderForTests
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractFileImportingScopeContextTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val renderDefaultImportingScope = Directives.RENDER_DEFAULT_IMPORTING_SCOPE in mainModule.testModule.directives

        analyseForTest(mainFile) {
            val ktScopeContext = mainFile.importingScopeContext

            val scopeContextStringRepresentation = render(ktScopeContext, renderDefaultImportingScope)
            val scopeContextStringRepresentationPretty = render(ktScopeContext, renderDefaultImportingScope, printPretty = true)

            testServices.assertions.assertEqualsToTestDataFileSibling(scopeContextStringRepresentation)
            testServices.assertions.assertEqualsToTestDataFileSibling(scopeContextStringRepresentationPretty, extension = ".pretty.txt")
        }
    }

    private fun KaSession.render(
        importingScope: KaScopeContext,
        renderDefaultImportingScope: Boolean,
        printPretty: Boolean = false,
    ): String = prettyPrint {
        renderForTests(useSiteSession, importingScope, this@prettyPrint, printPretty) { ktScopeKind ->
            when (ktScopeKind) {
                is KaScopeKind.PackageMemberScope -> false
                is KaScopeKind.DefaultSimpleImportingScope -> renderDefaultImportingScope
                is KaScopeKind.DefaultStarImportingScope -> renderDefaultImportingScope
                else -> true
            }
        }
    }

    private object Directives : SimpleDirectivesContainer() {
        val RENDER_DEFAULT_IMPORTING_SCOPE by directive("render default importing scope in test output")
    }
}
