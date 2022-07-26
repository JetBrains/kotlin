/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.analysis.api.components.RendererModifier
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.TestReferenceResolveResultRenderer.renderResolvedTo
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedSingleModuleTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.unwrapMultiReferences
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractReferenceResolveTest : AbstractAnalysisApiBasedSingleModuleTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            defaultDirectives {
                +ConfigurationDirectives.WITH_STDLIB
            }
            useDirectives(Directives)
            forTestsMatching("analysis/analysis-api/testData/referenceResolve/kDoc/*") {
                defaultDirectives {
                    +AnalysisApiTestDirectives.DISABLE_DEPENDED_MODE
                    +AnalysisApiTestDirectives.IGNORE_FE10
                }
            }
        }
    }

    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val mainKtFile = ktFiles.singleOrNull() ?: ktFiles.firstOrNull { it.name == "main.kt" } ?: ktFiles.first()
        val caretPosition = testServices.expressionMarkerProvider.getCaretPosition(mainKtFile)
        val ktReferences = findReferencesAtCaret(mainKtFile, caretPosition)
        if (ktReferences.isEmpty()) {
            testServices.assertions.fail { "No references at caret found" }
        }

        val resolvedTo =
            analyseForTest(ktReferences.first().element) {
                val symbols = ktReferences.flatMap { it.resolveToSymbols() }
                checkReferenceResultForValidity(ktReferences, module, testServices, symbols)
                renderResolvedTo(symbols, renderingOptions)
            }

        if (Directives.UNRESOLVED_REFERENCE in module.directives) {
            return
        }

        val actual = "Resolved to:\n$resolvedTo"
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private fun findReferencesAtCaret(mainKtFile: KtFile, caretPosition: Int): List<KtReference> =
        mainKtFile.findReferenceAt(caretPosition)?.unwrapMultiReferences().orEmpty().filterIsInstance<KtReference>()

    private fun KtAnalysisSession.checkReferenceResultForValidity(
        references: List<KtReference>,
        module: TestModule,
        testServices: TestServices,
        resolvedTo: List<KtSymbol>
    ) {
        if (Directives.UNRESOLVED_REFERENCE in module.directives) {
            testServices.assertions.assertTrue(resolvedTo.isEmpty()) {
                "Reference should be unresolved, but was resolved to ${renderResolvedTo(resolvedTo)}"
            }
        } else {
            if (resolvedTo.isEmpty()) {
                testServices.assertions.fail { "Unresolved reference ${references.first().element.text}" }
            }
        }
    }

    private object Directives : SimpleDirectivesContainer() {
        val UNRESOLVED_REFERENCE by directive(
            "Reference should be unresolved",
        )
    }

    private val renderingOptions = KtDeclarationRendererOptions.DEFAULT.copy(
        modifiers = RendererModifier.DEFAULT - RendererModifier.ANNOTATIONS,
        sortNestedDeclarations = true
    )

}
