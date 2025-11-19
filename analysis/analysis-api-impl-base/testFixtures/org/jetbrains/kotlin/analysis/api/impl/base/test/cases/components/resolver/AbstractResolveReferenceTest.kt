/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceService
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.containingDeclaration
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.TestReferenceResolveResultRenderer.renderResolvedTo
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaRendererAnnotationsFilter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.KaDeclarationModifiersRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers.KaModifierListRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.KaTypeParameterRendererFilter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables.KaPropertyAccessorsRenderer
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.FileMarker
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.services.toCaretMarker
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.analysis.test.framework.utils.unwrapMultiReferences
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfTypeInPreorder
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveReferenceTest : AbstractResolveTest<KtReference?>() {
    override val resolveKind: String get() = "references"

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(Directives)
            forTestsMatching("analysis/analysis-api/testData/components/resolver/singleByPsi/kDoc/*") {
                defaultDirectives {
                    +AnalysisApiTestDirectives.DISABLE_DEPENDED_MODE
                    +AnalysisApiTestDirectives.IGNORE_FE10
                }
            }
            forTestsMatching("analysis/analysis-api/testData/components/resolver/singleByPsi/kDoc/qualified/stdlib/*") {
                defaultDirectives {
                    +ConfigurationDirectives.WITH_STDLIB
                }
            }
        }
    }

    override fun collectElementsToResolve(
        file: KtFile,
        module: KtTestModule,
        testServices: TestServices,
    ): Collection<ResolveTestCaseContext<KtReference?>> {
        val caretPositions = testServices.expressionMarkerProvider.getAllCarets(file).takeIf { it.isNotEmpty() }
            ?: testServices.expressionMarkerProvider.getAllSelections(file).map { it.toCaretMarker() }

        return collectElementsToResolve(caretPositions, file)
    }

    protected fun collectElementsToResolve(
        carets: List<FileMarker<Int>>,
        file: KtFile,
    ): Collection<ResolveTestCaseContext<KtReference?>> = carets.flatMap { caret ->
        val marker = caret.tagText
        val contexts: List<ResolveTestCaseContext<KtReference?>> = findReferencesAtCaret(file, caret.value).map { reference ->
            ResolveReferenceTestCaseContext(element = reference, marker = marker)
        }

        contexts.ifEmpty {
            listOf(ResolveReferenceTestCaseContext(element = null, marker = marker))
        }
    }

    protected fun collectAllReferences(file: KtFile): Collection<ResolveReferenceTestCaseContext> = buildSet {
        val referenceService = PsiReferenceService.getService()
        file.forEachDescendantOfTypeInPreorder<PsiElement> { element ->
            for (reference in referenceService.getContributedReferences(element)) {
                if (reference !is KtReference) continue
                val context = ResolveReferenceTestCaseContext(element = reference, marker = null)
                add(context)
            }
        }
    }

    class ResolveReferenceTestCaseContext(
        override val element: KtReference?,
        override val marker: String?,
    ) : ResolveTestCaseContext<KtReference?> {
        override val context: KtElement? get() = element?.element
    }

    override fun generateResolveOutput(
        context: ResolveTestCaseContext<KtReference?>,
        file: KtFile,
        module: KtTestModule,
        testServices: TestServices,
    ): String {
        val reference = context.element ?: return "no references found"

        return analyzeReferenceElement(reference.element, module) {
            val symbols = reference.resolveToSymbols()
            val symbolsAgain = reference.resolveToSymbols()
            testServices.assertions.assertEquals(symbols, symbolsAgain)

            val isImplicitReferenceToCompanion = reference.isImplicitReferenceToCompanion()

            val resolvesByNamesViolations = resolvesByNamesViolations(file, reference, symbols, isImplicitReferenceToCompanion)

            val renderPsiClassName = Directives.RENDER_PSI_CLASS_NAME in module.testModule.directives
            val options = createRenderingOptions(renderPsiClassName)
            prettyPrint {
                appendLine("isImplicitReferenceToCompanion: $isImplicitReferenceToCompanion")
                appendLine("usesContextSensitiveResolution: ${reference.usesContextSensitiveResolution}")
                resolvesByNamesViolations?.let(::appendLine)
                appendLine("symbols:")
                withIndent {
                    val resolvedSymbolsInfo = renderResolvedTo(
                        symbols = symbols,
                        renderer = options,
                        sortRenderedDeclarations = reference !is KDocReference,
                    ) { getAdditionalSymbolInfo(it) }

                    append(resolvedSymbolsInfo)
                }
            }
        }
    }

    /**
     * Returns a string representation of the [KtReference.resolvesByNames] violations.
     *
     * Consider exdending the [KtReference.resolvesByNames]'s KDoc after modifications inside this method
     */
    context(_: KaSession)
    private fun resolvesByNamesViolations(
        file: KtFile,
        reference: KtReference,
        symbols: Collection<KaSymbol>,
        isImplicitReferenceToCompanion: Boolean,
    ): String? {
        // The stable order is required
        val providedNames = reference.resolvesByNames.map(Name::asString).toSet()
        val shouldNotPredictNames = when (reference) {
            is KtDefaultAnnotationArgumentReference, is KtConstructorDelegationReference -> true
            is KtSimpleNameReference -> when (val element = reference.element) {
                is KtNameReferenceExpression -> element.parent is KtInstanceExpressionWithLabel
                is KtLabelReferenceExpression -> true
                else -> false
            }

            is KDocReference -> when (reference.element.text) {
                KtTokens.THIS_KEYWORD.value, KtTokens.SUPER_KEYWORD.value -> true
                else -> false
            }

            else -> false
        }

        val violationMessagePrefix = "resolvesByNamesViolations:"
        if (shouldNotPredictNames) {
            return providedNames.takeIf { it.isNotEmpty() }?.let { providedNames ->
                "$violationMessagePrefix shouldn't predict, but $providedNames provided"
            }
        }

        val resolvedToAlias = file.importDirectives
            .filter { it.aliasName != null && it.importedFqName != null }
            .groupBy(
                keySelector = { it.importedFqName!!.shortName().asString() },
                valueTransform = { it.aliasName!! },
            )
            .toMap()

        val resolvedNames: Set<String> = symbols.mapNotNullTo(mutableSetOf()) { resolvedSymbol ->
            when (resolvedSymbol) {
                // Skip check for companion object usages since we cannot predict them
                is KaNamedClassSymbol if (resolvedSymbol.classKind == KaClassKind.COMPANION_OBJECT && isImplicitReferenceToCompanion) -> null

                is KaNamedSymbol -> resolvedSymbol.name.asString()

                // Constructors themselves don't have names, but we might check the containing class name since it is expected
                // to be inside the provided list
                is KaConstructorSymbol -> resolvedSymbol.containingDeclaration!!.name!!.asString()

                // Package symbol is not considered as a named one (at least yet)
                is KaPackageSymbol -> resolvedSymbol.fqName.shortName().asString()

                // Property accessors don't have names, but we might check the containing property name since it is expected
                // to be inside the provided list
                // Currently, only K1 cases are known to be present in the resolution result
                is KaPropertyAccessorSymbol ->
                    if (configurator.frontendKind == FrontendKind.Fe10) {
                        resolvedSymbol.containingDeclaration!!.name!!.asString()
                    } else {
                        // Most likely, this branch will need to be dropped once K2 has a use case for it
                        error("Unexpected symbol $resolvedSymbol. Most likely the KDoc of ${KtReference::resolvesByNames.name} should be updated to cover this case")
                    }

                else -> error("Unexpected symbol $resolvedSymbol")
            }
        }

        val violations = resolvedNames.mapNotNull { resolvedName ->
            if (resolvedName !in providedNames && resolvedToAlias[resolvedName].orEmpty().none { it in providedNames }) {
                "'$resolvedName'"
            } else {
                null
            }
        }.ifEmpty { return null }

        val violationsAsString = violations.singleOrNull() ?: violations.toString()
        val suffixWithAliases = if (resolvedToAlias.isNotEmpty()) " + $resolvedToAlias" else ""
        return "$violationMessagePrefix $violationsAsString !in $providedNames$suffixWithAliases"
    }

    private fun createRenderingOptions(renderPsiClassName: Boolean): KaDeclarationRenderer {
        if (!renderPsiClassName) return defaultRenderingOptions
        return defaultRenderingOptions.with {

            modifiersRenderer = modifiersRenderer.with {
                val delegateModifierListRenderer = modifierListRenderer
                modifierListRenderer = object : KaModifierListRenderer {
                    override fun renderModifiers(
                        analysisSession: KaSession,
                        symbol: KaDeclarationSymbol,
                        declarationModifiersRenderer: KaDeclarationModifiersRenderer,
                        printer: PrettyPrinter,
                    ) {
                        printer {
                            append("{psi: ${symbol.psi?.let { it::class.simpleName }}}")
                        }
                        delegateModifierListRenderer.renderModifiers(analysisSession, symbol, declarationModifiersRenderer, printer)
                    }
                }
            }
        }
    }


    protected open fun <R> analyzeReferenceElement(element: KtElement, module: KtTestModule, action: KaSession.() -> R): R {
        return analyzeForTest(element) { action() }
    }

    open fun KaSession.getAdditionalSymbolInfo(symbol: KaSymbol): String? = null

    private fun findReferencesAtCaret(file: KtFile, caretPosition: Int): List<KtReference> =
        file.findReferenceAt(caretPosition)?.unwrapMultiReferences().orEmpty().filterIsInstance<KtReference>()

    private object Directives : SimpleDirectivesContainer() {
        val RENDER_PSI_CLASS_NAME by directive(
            "Render also PSI class name for resolved reference"
        )
    }

    private val defaultRenderingOptions = KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES.with {
        annotationRenderer = annotationRenderer.with {
            annotationFilter = KaRendererAnnotationsFilter.NONE
        }
        propertyAccessorsRenderer = KaPropertyAccessorsRenderer.NONE
        typeParametersFilter = KaTypeParameterRendererFilter { _, _ -> true }
    }

}
