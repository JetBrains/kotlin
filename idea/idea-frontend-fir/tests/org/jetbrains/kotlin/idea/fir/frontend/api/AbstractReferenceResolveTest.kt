/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.frontend.api

import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import org.jetbrains.kotlin.idea.fir.frontend.api.test.framework.AbstractHLApiSingleModuleTest
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.expressionMarkerProvider
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.idea.frontend.api.components.RendererModifier
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractReferenceResolveTest : AbstractHLApiSingleModuleTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            defaultDirectives {
                +JvmEnvironmentConfigurationDirectives.WITH_STDLIB
            }
            useDirectives(Directives)
        }
    }

    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val mainKtFile = ktFiles.singleOrNull() ?: ktFiles.first { it.name == "main.kt" }
        val caretPosition = testServices.expressionMarkerProvider.getCaretPosition(mainKtFile)
        val ktReferences = findReferencesAtCaret(mainKtFile, caretPosition)
        if (ktReferences.isEmpty()) {
            testServices.assertions.fail { "No references at caret found" }
        }

        val resolvedTo = analyse(mainKtFile) {
            val symbols = ktReferences.flatMap { it.resolveToSymbols() }
            checkReferenceResultForValidity(module, testServices, symbols)
            renderResolvedTo(symbols)
        }

        if (Directives.UNRESOLVED_REFERENCE in module.directives) {
            return
        }

        val actual = "Resolved to:\n$resolvedTo"
        testServices.assertions.assertEqualsToFile(testDataFileSibling(".txt"), actual)
    }

    private fun findReferencesAtCaret(mainKtFile: KtFile, caretPosition: Int): List<KtReference> =
        mainKtFile.findReferenceAt(caretPosition)?.unwrapMultiReferences().orEmpty().filterIsInstance<KtReference>()

    private fun KtAnalysisSession.checkReferenceResultForValidity(
        module: TestModule,
        testServices: TestServices,
        resolvedTo: List<KtSymbol>
    ) {
        if (Directives.UNRESOLVED_REFERENCE in module.directives) {
            testServices.assertions.assertTrue(resolvedTo.isEmpty()) {
                "Reference should be unresolved, but was resolved to ${renderResolvedTo(resolvedTo)}"
            }
        } else {
            testServices.assertions.assertTrue(resolvedTo.isNotEmpty()) { "Unresolved reference" }
        }
    }

    private fun KtAnalysisSession.renderResolvedTo(symbols: List<KtSymbol>) =
        symbols.map { renderResolveResult(it) }
            .sorted()
            .withIndex()
            .joinToString(separator = "\n") { "${it.index}: ${it.value}" }


    private fun KtAnalysisSession.renderResolveResult(symbol: KtSymbol): String {
        return buildString {
            symbolFqName(symbol)?.let { fqName ->
                append("(in $fqName) ")
            }
            append(symbol.render(renderingOptions))
        }
    }

    @Suppress("unused")// KtAnalysisSession receiver
    private fun KtAnalysisSession.symbolFqName(symbol: KtSymbol): String? = when (symbol) {
        is KtCallableSymbol -> symbol.callableIdIfNonLocal?.asSingleFqName()?.parent()
        is KtClassLikeSymbol -> symbol.classIdIfNonLocal?.asSingleFqName()?.parent()
        is KtConstructorSymbol -> symbol.containingClassIdIfNonLocal?.asSingleFqName()
        else -> null
    }?.let { if (it == FqName.ROOT) "ROOT" else it.asString()}

    private object Directives : SimpleDirectivesContainer() {
        val UNRESOLVED_REFERENCE by directive(
            "Reference should be unresolved",
        )
    }

    private val renderingOptions = KtDeclarationRendererOptions.DEFAULT.copy(
        modifiers = RendererModifier.DEFAULT - RendererModifier.ANNOTATIONS
    )

    private fun PsiReference.unwrapMultiReferences(): List<PsiReference> = when (this) {
        is KtReference -> listOf(this)
        is PsiMultiReference -> references.flatMap { it.unwrapMultiReferences() }
        else -> error("Unexpected reference $this")
    }
}