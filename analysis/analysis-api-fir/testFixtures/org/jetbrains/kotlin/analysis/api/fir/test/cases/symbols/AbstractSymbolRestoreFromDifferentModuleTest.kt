/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.symbols

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.test.cases.symbols.SymbolRestoreFromDifferentModuleTestDirectives.SYMBOL_BY_JAVA_PSI
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.renderer.render
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.analysis.api.symbols.KaDebugRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.restoreSymbol
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.resolveSymbolPreferringCall
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.exceptions.unexpectedElementError

abstract class AbstractSymbolRestoreFromDifferentModuleTest : AbstractAnalysisApiBasedTest() {
    private val defaultRenderer = KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES

    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(SymbolRestoreFromDifferentModuleTestDirectives)

    override fun doTest(testServices: TestServices) {
        val element = testServices.expressionMarkerProvider.getBottommostElementsOfTypeAtCarets<KtElement>(testServices).single().first

        val restoreAt =
            testServices.expressionMarkerProvider.getBottommostElementsOfTypeAtCarets<KtElement>(
                testServices,
                qualifier = "restoreAt"
            ).single().first

        val project = element.project
        val elementModule = KotlinProjectStructureProvider.getModule(project, element, useSiteModule = null)
        val restoreAtModule = KotlinProjectStructureProvider.getModule(project, restoreAt, useSiteModule = null)
        val isSymbolByJavaPsi = SYMBOL_BY_JAVA_PSI in testServices.moduleStructure.allDirectives

        val [debugRendered, prettyRendered, pointer] = analyzeForTest(element) {
            val symbol = getSymbol(element, isSymbolByJavaPsi)
            val pointer = symbol.createPointer()
            Triple(KaDebugRenderer().render(useSiteSession, symbol), symbol.render(defaultRenderer), pointer)
        }
        configurator.doGlobalModuleStateModification(project)

        val [debugRenderedRestored, prettyRenderedRestored] = analyzeForTest(restoreAt) {
            val symbol = pointer.restoreSymbol()
            symbol?.let { KaDebugRenderer().render(useSiteSession, it) } to symbol?.render(defaultRenderer)
        }

        val actualDebug = prettyPrint {
            appendLine("Inital from ${elementModule.moduleDescription}:")
            appendLine(debugRendered)
            appendLine()
            appendLine("Restored in ${restoreAtModule.moduleDescription}:")
            appendLine(debugRenderedRestored ?: NOT_RESTORED)
        }
        testServices.assertions.assertEqualsToTestOutputFile(actualDebug)

        val actualPretty = prettyPrint {
            appendLine("Inital from ${elementModule.moduleDescription}:")
            appendLine(prettyRendered)
            appendLine()
            appendLine("Restored in ${restoreAtModule.moduleDescription}:")
            appendLine(prettyRenderedRestored ?: NOT_RESTORED)
        }
        testServices.assertions.assertEqualsToTestOutputFile(actualPretty, extension = ".pretty.txt")
    }

    /**
     * Returns the symbol of [element], which is a declaration or a reference, or the symbol of its Java declaration if
     * [isSymbolByJavaPsi] is `true`.
     */
    context(_: KaSession)
    private fun getSymbol(element: KtElement, isSymbolByJavaPsi: Boolean): KaDeclarationSymbol {
        val symbol = when (element) {
            is KtDeclaration -> element.symbol
            is KtReferenceExpression -> element.resolveSymbolPreferringCall() ?: error("Failed to resolve the reference at the caret")
            else -> unexpectedElementError<KtElement>(element)
        }

        val symbolToRestore = if (isSymbolByJavaPsi) symbol.symbolByJavaPsi else symbol
        return symbolToRestore as? KaDeclarationSymbol ?: error("Expected a declaration symbol, got $symbolToRestore")
    }

    companion object {
        private const val NOT_RESTORED = "<NOT RESTORED>"
    }
}

object SymbolRestoreFromDifferentModuleTestDirectives : SimpleDirectivesContainer() {
    val SYMBOL_BY_JAVA_PSI by directive(
        description = "Restore the symbol of the Java declaration of the symbol at the caret, as `symbolByJavaPsi` tests do",
    )
}
