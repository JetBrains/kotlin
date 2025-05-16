/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseIllegalPsiException
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseCachedSymbolPointer.Companion.isCacheable
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBasePsiSymbolPointer
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolTestDirectives.DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolTestDirectives.DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolTestDirectives.DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K2
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolTestDirectives.DO_NOT_CHECK_SYMBOL_RESTORE
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolTestDirectives.DO_NOT_CHECK_SYMBOL_RESTORE_K1
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolTestDirectives.DO_NOT_CHECK_SYMBOL_RESTORE_K2
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolTestDirectives.PRETTY_RENDERER_OPTION
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolTestDirectives.RENDER_IS_PUBLIC_API
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.KaClassifierBodyRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.KaExpandedTypeRenderingMode
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KaFunctionalTypeRenderer
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiMode
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.exceptions.KotlinIllegalArgumentExceptionWithAttachments
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.opentest4j.AssertionFailedError
import java.util.concurrent.ExecutionException
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField
import kotlin.test.fail

abstract class AbstractSymbolTest : AbstractAnalysisApiBasedTest() {
    /**
     * Currently [KaFileSymbol] cannot be restored without a backed PSI element,
     * so it is better to suppress it to not hide other problems.
     */
    open val suppressPsiBasedFilePointerCheck: Boolean get() = true

    open val defaultRenderer = KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES

    open val defaultRendererOption: PrettyRendererOption? = null

    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(SymbolTestDirectives)

    abstract fun KaSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        testServices.moduleStructure.allDirectives.suppressIf(
            suppressionDirective = SymbolTestDirectives.ILLEGAL_PSI,
            filter = Throwable::isIllegalPsiException,
        ) {
            doTestByMainFile(mainFile, mainModule, testServices, disablePsiBasedLogic = false)
            doTestByMainFile(mainFile, mainModule, testServices, disablePsiBasedLogic = true)
        }
    }

    private fun doTestByMainFile(
        mainFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices,
        disablePsiBasedLogic: Boolean,
    ) {
        val markerProvider = testServices.expressionMarkerProvider
        val analyzeContext = testServices.ktTestModuleStructure.allMainKtFiles.firstNotNullOfOrNull {
            markerProvider.getBottommostElementOfTypeAtCaretOrNull<KtElement>(it, "context")
        }

        val directives = mainModule.testModule.directives
        val directiveToIgnore = directives.doNotCheckNonPsiSymbolRestoreDirective()?.takeIf { disablePsiBasedLogic }
            ?: directives.doNotCheckSymbolRestoreDirective()

        val prettyRenderer = buildList {
            addIfNotNull(defaultRendererOption)
            addAll(directives[PRETTY_RENDERER_OPTION])
        }.fold(defaultRenderer) { acc, prettyRenderingMode ->
            prettyRenderingMode.transformation(acc)
        }

        fun KaSession.safePointer(ktSymbol: KaSymbol): KaSymbolPointer<*>? {
            if (disablePsiBasedLogic && ktSymbol is KaFileSymbol && suppressPsiBasedFilePointerCheck) return null

            val result = ktSymbol.runCatching {
                createPointerForTest(disablePsiBasedLogic = disablePsiBasedLogic)
            }

            val pointer = when {
                directiveToIgnore != null -> result.getOrNull()
                else -> result.getOrThrow()
            } ?: return null

            assertSymbolPointer(pointer, testServices)
            return pointer
        }

        val pointersWithRendered = executeOnPooledThreadInReadAction {
            analyzeForTest(analyzeContext ?: mainFile) {
                val (symbols, symbolForPrettyRendering) = collectSymbols(mainFile, testServices).also {
                    if (disablePsiBasedLogic) {
                        it.dropBackingPsi()
                    }
                }

                checkContainingFiles(symbols, mainFile, testServices)

                val pointerWithRenderedSymbol = symbols
                    .asSequence()
                    .flatMap { symbol ->
                        sequenceOf(symbol to true) + symbol.withImplicitSymbols().map { implicitSymbol ->
                            if (disablePsiBasedLogic) {
                                implicitSymbol.dropBackingPsi()
                            }

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

        compareResults(pointersWithRendered, testServices, disablePsiBasedLogic)

        configurator.doGlobalModuleStateModification(mainFile.project)

        restoreSymbolsInOtherReadActionAndCompareResults(
            directiveToIgnore = directiveToIgnore,
            ktFile = mainFile,
            pointersWithRendered = pointersWithRendered.pointers,
            testServices = testServices,
            directives = directives,
            disablePsiBasedLogic = disablePsiBasedLogic,
            analyzeContext = analyzeContext,
        )
    }

    private fun KaSymbol.createPointerForTest(disablePsiBasedLogic: Boolean): KaSymbolPointer<*> =
        KaBasePsiSymbolPointer.withDisabledPsiBasedPointers(disable = disablePsiBasedLogic) { createPointer() }

    private fun assertSymbolPointer(pointer: KaSymbolPointer<*>, testServices: TestServices) {
        testServices.assertions.assertTrue(value = pointer.pointsToTheSameSymbolAs(pointer)) {
            "The symbol is not equal to itself: ${pointer::class}"
        }
    }

    private fun KaSession.checkContainingFiles(symbols: List<KaSymbol>, mainFile: KtFile, testServices: TestServices) {
        val allowedContainingFileSymbols = getAllowedContainingFiles(mainFile, testServices).mapToSetOrEmpty {
            it.takeIf { it.canBeAnalysed() }?.symbol
        }

        for (symbol in symbols) {
            if (symbol.origin != KaSymbolOrigin.SOURCE) continue

            val containingFileSymbol = symbol.containingFile
            when {
                symbol is KaFileSymbol -> {
                    testServices.assertions.assertEquals(null, containingFileSymbol) {
                        "'containingFile' for ${KaFileSymbol::class.simpleName} should be 'null'"
                    }
                }

                containingFileSymbol !in allowedContainingFileSymbols -> {
                    testServices.assertions.fail {
                        "Invalid file for `$symbol`: Found `$containingFileSymbol`, which is not an allowed file symbol."
                    }
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
        disablePsiBasedLogic: Boolean,
    ) {
        val actual = data.pointers.renderDeclarations()
        compareResults(actual, testServices, disablePsiBasedLogic, extension = "txt")

        val actualPretty = data.pointersForPrettyRendering.renderDeclarations()
        compareResults(actualPretty, testServices, disablePsiBasedLogic, extension = "pretty.txt")
    }

    private fun compareResults(actual: String, testServices: TestServices, disablePsiBasedLogic: Boolean, extension: String) {
        val assertions = testServices.assertions
        if (!disablePsiBasedLogic) {
            assertions.assertEqualsToTestOutputFile(actual = actual, extension = extension)
        } else {
            val expectedFile = getTestOutputFile(extension).toFile()
            val nonPsiExpectedFile = getTestOutputFile("nonPsi.$extension").toFile()
            when {
                assertions.doesEqualToFile(expectedFile, actual) -> {
                    if (nonPsiExpectedFile.exists() &&
                        configurator.frontendKind == FrontendKind.Fir &&
                        configurator.analysisApiMode == AnalysisApiMode.Ide
                    ) {
                        throw AssertionError("'${nonPsiExpectedFile.path}' should be removed as the actual output is the same as '${expectedFile.path}'")
                    }
                }

                else -> {
                    if (nonPsiExpectedFile.exists() && configurator.frontendKind == FrontendKind.Fir) {
                        assertions.assertEqualsToFile(nonPsiExpectedFile, actual)
                        return
                    }

                    val message = """
                        Non-PSI version doesn't equal to the PSI-based variation.
                        If you want to commit both results, please add a separate file "${expectedFile.nameWithoutExtension}.nonPsi.txt".
                        """.trimIndent()
                    throw AssertionFailedError(
                        /* message = */ message,
                        /* expected = */ expectedFile.readText(),
                        /* actual = */ actual,
                    )
                }
            }
        }
    }

    private fun List<PointerWithRenderedSymbol>.renderDeclarations(): String =
        mapNotNull { it.rendered.takeIf { _ -> it.shouldBeRendered } }.renderAsDeclarations()

    private fun List<String>.renderAsDeclarations(): String =
        if (isEmpty()) "NO_SYMBOLS"
        else joinToString(separator = "\n\n")

    private fun restoreSymbolsInOtherReadActionAndCompareResults(
        directiveToIgnore: Directive?,
        ktFile: KtFile,
        pointersWithRendered: List<PointerWithRenderedSymbol>,
        testServices: TestServices,
        directives: RegisteredDirectives,
        disablePsiBasedLogic: Boolean,
        analyzeContext: KtElement?,
    ) {
        var failed = false
        val restoredPointers = mutableListOf<KaSymbolPointer<*>>()
        try {
            val restored = analyzeForTest(analyzeContext ?: ktFile) {
                pointersWithRendered.mapNotNull { (pointer, expectedRender, shouldBeRendered) ->
                    val pointer = pointer ?: error("Symbol pointer was not created for symbol:\n$expectedRender")
                    val restored = restoreSymbol(pointer, disablePsiBasedLogic) ?: error("Symbol was not restored:\n$expectedRender")
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
            val expectedFile = getTestOutputFile().toFile()
            if (!testServices.assertions.doesEqualToFile(expectedFile, actual)) {
                error("Restored content is not the same. Actual:\n$actual")
            }
        } catch (e: Throwable) {
            if (directiveToIgnore == null) throw e
            failed = true
        }

        if (!failed) {
            compareCachedSymbols(restoredPointers, testServices, ktFile, disablePsiBasedLogic, analyzeContext)
            compareRestoredSymbols(restoredPointers, testServices, ktFile, disablePsiBasedLogic, analyzeContext)
        }

        // Do not fail for standalone as the IDE mode may have different behavior and it is primary
        if (failed || directiveToIgnore == null || configurator.analysisApiMode == AnalysisApiMode.Standalone) return

        fail("'// ${directiveToIgnore.name}' directive has no effect on the test")
    }

    private fun compareCachedSymbols(
        pointers: List<KaSymbolPointer<*>>,
        testServices: TestServices,
        ktFile: KtFile,
        disablePsiBasedLogic: Boolean,
        analyzeContext: KtElement?,
    ) {
        if (pointers.isEmpty()) return

        val contextElement = analyzeContext ?: ktFile
        analyzeForTest(contextElement) {
            pointers.forEach { pointer ->
                val firstRestore =
                    restoreSymbol(pointer, disablePsiBasedLogic)
                        ?: error("Unexpectedly non-restored symbol pointer: ${contextElement::class}")

                val secondRestore =
                    restoreSymbol(pointer, disablePsiBasedLogic)
                        ?: error("Unexpectedly non-restored symbol pointer: ${contextElement::class}")

                if (firstRestore.isCacheable) {
                    testServices.assertions.assertTrue(firstRestore === secondRestore) {
                        "${pointer::class} does not support symbol caching"
                    }
                }
            }
        }
    }

    private fun compareRestoredSymbols(
        restoredPointers: List<KaSymbolPointer<*>>,
        testServices: TestServices,
        ktFile: KtFile,
        disablePsiBasedLogic: Boolean,
        analyzeContext: KtElement?,
    ) {
        if (restoredPointers.isEmpty()) return

        analyzeForTest(analyzeContext ?: ktFile) {
            val symbolsToPointersMap = restoredPointers.groupByTo(mutableMapOf()) {
                restoreSymbol(it, disablePsiBasedLogic) ?: error("Unexpectedly non-restored symbol pointer: ${it::class}")
            }

            val pointersToCheck = symbolsToPointersMap.map { (key, value) ->
                value += key.createPointerForTest(disablePsiBasedLogic = disablePsiBasedLogic)
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
        val renderer = DebugSymbolRenderer(
            renderExtra = true,
            renderExpandedTypes = directives[PRETTY_RENDERER_OPTION].any { it == PrettyRendererOption.FULLY_EXPANDED_TYPES },
            renderIsPublicApi = RENDER_IS_PUBLIC_API in directives
        )
        return with(renderer) { render(useSiteSession, symbol) }
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

    val ILLEGAL_PSI by stringDirective(description = "Symbol should not be created for this PSI element")

    val RENDER_IS_PUBLIC_API by directive(description = "Render `isPublicApi` attribute for symbols")
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
                    functionalTypeRenderer = KaFunctionalTypeRenderer.AS_CLASS_TYPE_FOR_REFLECTION_TYPES_WITH_PARAMETER_NAMES
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
        is KtParameter -> !isFunctionTypeParameter && ownerDeclaration == null
        is KtNamedFunction -> name != null
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
    val pointer: KaSymbolPointer<*>?,
    val rendered: String,
    val shouldBeRendered: Boolean = true,
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

        if (ktSymbol is KaCallableSymbol) {
            for (parameter in ktSymbol.contextParameters) {
                yieldAll(parameter.withImplicitSymbols())
            }

            yieldAll(ktSymbol.receiverParameter.withImplicitSymbols())
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

private fun <S : KaSymbol> KaSession.restoreSymbol(pointer: KaSymbolPointer<S>, disablePsiBasedLogic: Boolean): S? {
    val symbol = pointer.restoreSymbol() ?: return null
    if (disablePsiBasedLogic) {
        symbol.dropBackingPsi()
    }

    return symbol
}

private fun SymbolsData.dropBackingPsi() {
    symbols.forEach(KaSymbol::dropBackingPsi)
    symbolsForPrettyRendering.forEach(KaSymbol::dropBackingPsi)
}

/**
 * Some K2 implementations of [KaSymbol] is backed by some [PsiElement],
 * so they may implement some API on top of PSI, FirSymbols or both of them.
 *
 * FirSymbol-based implementation is the source of truth, so the PSI-based implementation
 * exists to cover simple cases.
 *
 * As most of the symbols have the underlying PSI element, it is crucial to
 * have consistent implementation for PSI-based and FirSymbol-based symbols.
 */
private fun KaSymbol.dropBackingPsi() {
    val interfaceInstance = Class.forName("org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirPsiSymbol")
    val symbolType = KaSymbol::class.createType()

    val thisClass = this::class
    for (property in thisClass.declaredMemberProperties) {
        // Some symbols may have owning symbols, so they should be invalidated as well
        if (!property.name.startsWith("owning") || !property.returnType.isSubtypeOf(symbolType)) continue

        val symbol = property.getter.call(this) as KaSymbol
        symbol.dropBackingPsi()
    }

    if (!interfaceInstance.isInstance(this)) return

    when (thisClass.simpleName) {
        // Those classes are PSI-based only, so they have FirSymbol only for the compatibility with other classes
        "KaFirPsiJavaClassSymbol",
        "KaFirPsiJavaTypeParameterSymbol",
            -> return

        // There classes depend on the property PSI. The owning property is already invalidated above
        "KaFirDefaultPropertyGetterSymbol",
        "KaFirDefaultPropertySetterSymbol",
        "KaFirPropertyGetterSymbol",
        "KaFirPropertySetterSymbol",
            -> return
    }

    val property = thisClass.memberProperties.single { it.name == "backingPsi" }
    val returnType = property.returnType
    if (!returnType.isSubtypeOf(PsiElement::class.createType().withNullability(true))) {
        error("Unexpected return type '$returnType' for '${this::class.simpleName}' class")
    }

    val field = property.javaField ?: error("Backing field is not found")
    field.isAccessible = true

    // Drop backing PSI to trigger non-psi implementation
    field.set(this, null)
}

private val Throwable.isIllegalPsiException: Boolean
    get() = when (this) {
        is KaBaseIllegalPsiException -> true
        is ExecutionException -> cause?.isIllegalPsiException == true
        is KotlinIllegalArgumentExceptionWithAttachments -> {
            message?.startsWith("Creating ${KaVariableSymbol::class.simpleName} for function type parameter is not possible.") == true
        }

        else -> false
    }
