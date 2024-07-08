/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolTestDirectives.DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolTestDirectives.DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolTestDirectives.DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K2
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolTestDirectives.DO_NOT_CHECK_SYMBOL_RESTORE
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolTestDirectives.DO_NOT_CHECK_SYMBOL_RESTORE_K1
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolTestDirectives.DO_NOT_CHECK_SYMBOL_RESTORE_K2
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolTestDirectives.PRETTY_RENDERER_OPTION
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.KaClassifierBodyRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.KaExpandedTypeRenderingMode
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KaFunctionalTypeRenderer
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import kotlin.test.fail

abstract class AbstractSymbolTest : AbstractAnalysisApiBasedTest() {
    open val defaultRenderer = KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES

    open val defaultRendererOption: PrettyRendererOption? = null

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(SymbolTestDirectives)
        }
    }

    abstract fun KaSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val directives = mainModule.testModule.directives
        val directiveToIgnoreSymbolRestore = directives.doNotCheckSymbolRestoreDirective()
        val directiveToIgnoreNonPsiSymbolRestore = directives.doNotCheckNonPsiSymbolRestoreDirective()

        val prettyRenderer = buildList {
            addIfNotNull(defaultRendererOption)
            addAll(directives[PRETTY_RENDERER_OPTION])
        }.fold(defaultRenderer) { acc, prettyRenderingMode ->
            prettyRenderingMode.transformation(acc)
        }

        fun KaSession.safePointer(ktSymbol: KaSymbol): PointerWrapper? {
            val regularPointer = ktSymbol.runCatching {
                createPointerForTest(disablePsiBasedSymbols = false)
            }.let {
                if (directiveToIgnoreSymbolRestore == null) it.getOrThrow() else it.getOrNull()
            } ?: return null

            assertSymbolPointer(regularPointer, testServices)
            val nonPsiPointer = ktSymbol.runCatching {
                if (this is KaFileSymbol) return@runCatching null
                createPointerForTest(disablePsiBasedSymbols = true)
            }

            val pointerWithoutPsiAnchor = if (directiveToIgnoreSymbolRestore == null && directiveToIgnoreNonPsiSymbolRestore == null)
                nonPsiPointer.getOrThrow()
            else
                nonPsiPointer.getOrNull()

            if (pointerWithoutPsiAnchor != null) {
                assertSymbolPointer(pointerWithoutPsiAnchor, testServices)
            }

            return PointerWrapper(regularPointer = regularPointer, pointerWithoutPsiAnchor = pointerWithoutPsiAnchor)
        }

        val pointersWithRendered = executeOnPooledThreadInReadAction {
            analyseForTest(mainFile) {
                val (symbols, symbolForPrettyRendering) = collectSymbols(mainFile, testServices)

                checkContainingFiles(symbols, mainFile, testServices)

                val pointerWithRenderedSymbol = symbols
                    .asSequence()
                    .flatMap { symbol ->
                        sequenceOf(symbol to true) + symbol.withImplicitSymbols().map { implicitSymbol ->
                            implicitSymbol to false
                        }
                    }
                    .distinctBy { it.first }
                    .map { (symbol, shouldBeRendered) ->
                        PointerWithRenderedSymbol(
                            pointer = safePointer(symbol),
                            rendered = renderSymbolForComparison(symbol, directives),
                            shouldBeRendered = shouldBeRendered,
                        )
                    }
                    .toList()

                val pointerWithPrettyRenderedSymbol = symbolForPrettyRendering.map { symbol ->
                    PointerWithRenderedSymbol(
                        safePointer(symbol),
                        when (symbol) {
                            is KaReceiverParameterSymbol -> DebugSymbolRenderer().render(useSiteSession, symbol)
                            is KaDeclarationSymbol -> symbol.render(prettyRenderer)
                            is KaFileSymbol -> prettyPrint {
                                printCollection(symbol.fileScope.declarations.asIterable(), separator = "\n\n") {
                                    append(it.render(prettyRenderer))
                                }
                            }
                            else -> error(symbol::class.toString())
                        },
                    )
                }

                SymbolPointersData(pointerWithRenderedSymbol, pointerWithPrettyRenderedSymbol)
            }
        }

        compareResults(pointersWithRendered, testServices)

        configurator.doGlobalModuleStateModification(mainFile.project)

        restoreSymbolsInOtherReadActionAndCompareResults(
            directiveToIgnore = directiveToIgnoreSymbolRestore,
            isRegularPointers = true,
            ktFile = mainFile,
            pointersWithRendered = pointersWithRendered.pointers,
            testServices = testServices,
            directives = directives,
        )

        if (directiveToIgnoreSymbolRestore == null) {
            restoreSymbolsInOtherReadActionAndCompareResults(
                directiveToIgnore = directiveToIgnoreNonPsiSymbolRestore,
                isRegularPointers = false,
                ktFile = mainFile,
                pointersWithRendered = pointersWithRendered.pointers,
                testServices = testServices,
                directives = directives,
            )
        }
    }

    private fun KaSymbol.createPointerForTest(disablePsiBasedSymbols: Boolean): KaSymbolPointer<*> =
        KaPsiBasedSymbolPointer.withDisabledPsiBasedPointers(disable = disablePsiBasedSymbols) { createPointer() }

    private fun assertSymbolPointer(pointer: KaSymbolPointer<*>, testServices: TestServices) {
        testServices.assertions.assertTrue(value = pointer.pointsToTheSameSymbolAs(pointer)) {
            "The symbol is not equal to itself: ${pointer::class}"
        }
    }

    private fun KaSession.checkContainingFiles(symbols: List<KaSymbol>, mainFile: KtFile, testServices: TestServices) {
        val allowedContainingFileSymbols = getAllowedContainingFiles(mainFile, testServices).mapToSetOrEmpty { it.symbol }

        for (symbol in symbols) {
            if (symbol.origin != KaSymbolOrigin.SOURCE) continue

            val containingFileSymbol = symbol.containingFile
            if (containingFileSymbol !in allowedContainingFileSymbols) {
                testServices.assertions.fail {
                    "Invalid file for `$symbol`: Found `$containingFileSymbol`, which is not an allowed file symbol."
                }
            }
        }
    }

    /**
     * Returns the set of [KtFile]s which may contain any of the found symbols. If a symbol is not contained in one of these files, the test
     * fails.
     */
    open fun getAllowedContainingFiles(mainFile: KtFile, testServices: TestServices): Set<KtFile> = setOf(mainFile)

    private fun RegisteredDirectives.doNotCheckSymbolRestoreDirective(): Directive? = findSpecificDirective(
        commonDirective = DO_NOT_CHECK_SYMBOL_RESTORE,
        k1Directive = DO_NOT_CHECK_SYMBOL_RESTORE_K1,
        k2Directive = DO_NOT_CHECK_SYMBOL_RESTORE_K2,
    )

    private fun RegisteredDirectives.doNotCheckNonPsiSymbolRestoreDirective(): Directive? = findSpecificDirective(
        commonDirective = DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE,
        k1Directive = DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1,
        k2Directive = DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K2,
    )

    private fun compareResults(
        data: SymbolPointersData,
        testServices: TestServices,
    ) {
        val actual = data.pointers.renderDeclarations()
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)

        val actualPretty = data.pointersForPrettyRendering.renderDeclarations()
        testServices.assertions.assertEqualsToTestDataFileSibling(actualPretty, extension = ".pretty.txt")
    }

    private fun List<PointerWithRenderedSymbol>.renderDeclarations(): String =
        mapNotNull { it.rendered.takeIf { _ -> it.shouldBeRendered } }.renderAsDeclarations()

    private fun List<String>.renderAsDeclarations(): String =
        if (isEmpty()) "NO_SYMBOLS"
        else joinToString(separator = "\n\n")

    private fun restoreSymbolsInOtherReadActionAndCompareResults(
        directiveToIgnore: Directive?,
        isRegularPointers: Boolean,
        ktFile: KtFile,
        pointersWithRendered: List<PointerWithRenderedSymbol>,
        testServices: TestServices,
        directives: RegisteredDirectives,
    ) {
        var failed = false
        val restoredPointers = mutableListOf<KaSymbolPointer<*>>()
        try {
            val restored = analyseForTest(ktFile) {
                pointersWithRendered.mapNotNull { (pointerWrapper, expectedRender, shouldBeRendered) ->
                    val pointer = if (isRegularPointers) {
                        pointerWrapper?.regularPointer
                    } else {
                        pointerWrapper?.pointerWithoutPsiAnchor
                    } ?: error("Symbol pointer for $expectedRender was not created")

                    val restored = pointer.restoreSymbol() ?: error("Symbol $expectedRender was not restored")
                    restoredPointers += pointer

                    val actualRender = renderSymbolForComparison(restored, directives)
                    if (shouldBeRendered) {
                        actualRender
                    } else {
                        testServices.assertions.assertEquals(expectedRender, actualRender) { "${restored::class}" }
                        null
                    }
                }
            }

            val actual = restored.renderAsDeclarations()
            val expectedFile = getTestDataFileSiblingPath().toFile()
            if (!testServices.assertions.doesEqualToFile(expectedFile, actual)) {
                error("Restored content is not the same. Actual:\n$actual")
            }
        } catch (e: Throwable) {
            if (directiveToIgnore == null) throw e
            failed = true
        }

        if (!failed) {
            compareRestoredSymbols(restoredPointers, testServices, ktFile, isRegularPointers)
        }

        if (failed || directiveToIgnore == null) return

        testServices.assertions.assertEqualsToTestDataFileSibling(
            actual = ktFile.text.lines().filterNot { it == "// ${directiveToIgnore.name}" }.joinToString(separator = "\n"),
            extension = "kt",
        )

        fail("Redundant // ${directiveToIgnore.name} directive")
    }

    private fun compareRestoredSymbols(
        restoredPointers: List<KaSymbolPointer<*>>,
        testServices: TestServices,
        ktFile: KtFile,
        isRegularPointers: Boolean,
    ) {
        if (restoredPointers.isEmpty()) return

        analyseForTest(ktFile) {
            val symbolsToPointersMap = restoredPointers.groupByTo(mutableMapOf()) {
                it.restoreSymbol() ?: error("Unexpectedly non-restored symbol pointer: ${it::class}")
            }

            val pointersToCheck = symbolsToPointersMap.map { (key, value) ->
                value += if (isRegularPointers) {
                    key.createPointerForTest(disablePsiBasedSymbols = false)
                } else {
                    key.createPointerForTest(disablePsiBasedSymbols = true)
                }

                value
            }

            for (pointers in pointersToCheck) {
                for (firstPointer in pointers) {
                    for (secondPointer in pointers) {
                        testServices.assertions.assertTrue(firstPointer.pointsToTheSameSymbolAs(secondPointer)) {
                            "${firstPointer::class} is not the same as ${secondPointer::class}"
                        }
                    }
                }
            }
        }
    }

    protected open fun KaSession.renderSymbolForComparison(symbol: KaSymbol, directives: RegisteredDirectives): String {
        val renderExpandedTypes = directives[PRETTY_RENDERER_OPTION].any { it == PrettyRendererOption.FULLY_EXPANDED_TYPES }
        return with(DebugSymbolRenderer(renderExtra = true, renderExpandedTypes = renderExpandedTypes)) { render(useSiteSession, symbol) }
    }
}

object SymbolTestDirectives : SimpleDirectivesContainer() {
    val DO_NOT_CHECK_SYMBOL_RESTORE by directive(
        description = "Symbol restoring for some symbols in current test is not supported yet",
    )

    val DO_NOT_CHECK_SYMBOL_RESTORE_K1 by directive(
        description = "Symbol restoring for some symbols in current test is not supported yet in K1",
    )

    val DO_NOT_CHECK_SYMBOL_RESTORE_K2 by directive(
        description = "Symbol restoring for some symbols in current test is not supported yet in K2",
    )

    val DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE by directive(
        description = "Symbol restoring w/o psi for some symbols in current test is not supported yet",
    )

    val DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1 by directive(
        description = "Symbol restoring w/o psi for some symbols in current test is not supported yet in K1",
    )

    val DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K2 by directive(
        description = "Symbol restoring w/o psi for some symbols in current test is not supported yet in K2",
    )

    val PRETTY_RENDERER_OPTION by enumDirective(description = "Explicit rendering mode") { PrettyRendererOption.valueOf(it) }

    val TARGET_FILE_NAME by stringDirective(description = "The name of the main file")
}

enum class PrettyRendererOption(val transformation: (KaDeclarationRenderer) -> KaDeclarationRenderer) {
    BODY_WITH_MEMBERS(
        { renderer ->
            renderer.with {
                classifierBodyRenderer = KaClassifierBodyRenderer.BODY_WITH_MEMBERS
            }
        }
    ),
    FULLY_EXPANDED_TYPES(
        { renderer ->
            renderer.with {
                typeRenderer = typeRenderer.with {
                    expandedTypeRenderingMode = KaExpandedTypeRenderingMode.RENDER_EXPANDED_TYPE
                    functionalTypeRenderer = KaFunctionalTypeRenderer.AS_CLASS_TYPE_FOR_REFLECTION_TYPES
                }
            }
        }
    )
}

internal val KtDeclaration.isValidForSymbolCreation
    get() = when (this) {
        is KtBackingField -> false
        is KtDestructuringDeclaration -> false
        is KtPropertyAccessor -> false
        is KtParameter -> !this.isFunctionTypeParameter && this.parent !is KtParameterList
        is KtNamedFunction -> this.name != null
        else -> true
    }

data class SymbolsData(
    val symbols: List<KaSymbol>,
    val symbolsForPrettyRendering: List<KaSymbol> = symbols,
)

private data class SymbolPointersData(
    val pointers: List<PointerWithRenderedSymbol>,
    val pointersForPrettyRendering: List<PointerWithRenderedSymbol>,
)

private data class PointerWithRenderedSymbol(
    val pointer: PointerWrapper?,
    val rendered: String,
    val shouldBeRendered: Boolean = true,
)

private data class PointerWrapper(
    val regularPointer: KaSymbolPointer<*>,
    val pointerWithoutPsiAnchor: KaSymbolPointer<*>?,
)

private fun KaSymbol?.withImplicitSymbols(): Sequence<KaSymbol> {
    val ktSymbol = this ?: return emptySequence()
    return sequence {
        yield(ktSymbol)

        if (ktSymbol is KaDeclarationSymbol) {
            for (parameter in ktSymbol.typeParameters) {
                yieldAll(parameter.withImplicitSymbols())
            }
        }

        if (ktSymbol is KaPropertySymbol) {
            yieldAll(ktSymbol.getter.withImplicitSymbols())
            yieldAll(ktSymbol.setter.withImplicitSymbols())
        }

        if (ktSymbol is KaFunctionSymbol) {
            for (parameter in ktSymbol.valueParameters) {
                yieldAll(parameter.withImplicitSymbols())
            }
        }

        if (ktSymbol is KaValueParameterSymbol) {
            yieldAll(ktSymbol.generatedPrimaryConstructorProperty.withImplicitSymbols())
        }
    }
}
