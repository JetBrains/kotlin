/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionAttempt
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfTypeInPreorder
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractResolveByElementTest : AbstractResolveTest<KtElement>() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.apply {
            useDirectives(Directives, ResolveSymbolTestDirectives)
            forTestsMatching("analysis/analysis-api/testData/components/resolver/singleByPsi/kDoc/*") {
                defaultDirectives {
                    +Directives.BOTTOMMOST_ELEMENT
                    +AnalysisApiTestDirectives.DISABLE_DEPENDED_MODE
                }
            }

            forTestsMatching("analysis/analysis-api/testData/components/resolver/singleByPsi/kDoc/qualified/stdlib/*") {
                defaultDirectives {
                    +ConfigurationDirectives.WITH_STDLIB
                }
            }
        }
    }

    final override fun generateResolveOutput(
        resolveKind: ResolveKind,
        context: ResolveTestCaseContext<KtElement>,
        file: KtFile,
        module: KtTestModule,
        testServices: TestServices,
    ): String = generateResolveOutput(resolveKind, context.element, testServices)

    protected open fun generateResolveOutput(
        resolveKind: ResolveKind,
        mainElement: KtElement,
        testServices: TestServices,
    ): String = when (resolveKind) {
        ResolveKind.CALL -> analyzeForTest(mainElement) { generateCallOutput(mainElement, testServices) }
        ResolveKind.CANDIDATES -> analyzeForTest(mainElement) { generateCandidatesOutput(mainElement, testServices) }
        ResolveKind.SYMBOL -> analyzeSymbolElement(mainElement, testServices) { generateSymbolOutput(mainElement, testServices) }
    }

    open fun <R> analyzeSymbolElement(element: KtElement, testServices: TestServices, action: context(KaSession) () -> R): R {
        return analyzeForTest(element, action)
    }

    /**
     * Additional information about the resolved symbols, rendered in the [ResolveKind.SYMBOL] output.
     */
    context(session: KaSession)
    open fun additionalSymbolInfo(attempt: KaSymbolResolutionAttempt): String? = null

    override fun collectElementsToResolve(
        file: KtFile,
        module: KtTestModule,
        testServices: TestServices,
    ): Collection<ResolveTestCaseContext<KtElement>> {
        val carets = testServices.expressionMarkerProvider.getAllCarets(file)
        if (carets.size > 1 || carets.isNotEmpty() && Directives.BOTTOMMOST_ELEMENT in module.testModule.directives) {
            return carets.map {
                val element = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtElement>(file, it.qualifier)
                ResolveKtElementTestCaseContext(element = element, marker = it.tagText)
            }
        }

        val expression = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaretOrNull<KtExpression>(file)
            ?: testServices.expressionMarkerProvider.getTopmostSelectedElementOfTypeByDirectiveOrNull(
                file = file,
                module = module,
                defaultType = KtElement::class,
            ) as KtElement?

        if (expression == null) return emptyList()

        val elementToResolve = expression.elementToResolve
        return listOf(ResolveKtElementTestCaseContext(element = elementToResolve, marker = null))
    }

    class ResolveKtElementTestCaseContext(
        override val element: KtElement,
        override val marker: String?,
    ) : ResolveTestCaseContext<KtElement> {
        override val context: KtElement? get() = element
    }

    protected val KtElement.elementToResolve: KtElement
        get() = when (this) {
            is KtValueArgument -> getArgumentExpression()!!
            is KtDeclarationModifierList -> annotationEntries.singleOrNull() ?: error("Only single annotation entry is supported for now")
            is KtFileAnnotationList -> annotationEntries.singleOrNull() ?: error("Only single annotation entry is supported for now")
            else -> this
        }

    protected fun collectAllKtElements(file: KtFile): Collection<ResolveKtElementTestCaseContext> = buildSet {
        file.forEachDescendantOfTypeInPreorder<PsiElement> { element ->
            if (element is KtElement) {
                add(ResolveKtElementTestCaseContext(element = element, marker = null))
            }
        }
    }

    protected object Directives : SimpleDirectivesContainer() {
        val BOTTOMMOST_ELEMENT by directive(
            "Forces the test to search for the bottommost element at the caret position"
        )
    }
}

/**
 * Checks call, candidate, and symbol resolution of the elements at carets or selections at once, so the test setup is shared.
 */
abstract class AbstractResolveSingleByPsiTest : AbstractResolveByElementTest() {
    override val resolveKinds: List<ResolveKind> get() = ResolveKind.entries
}

/**
 * Checks call, candidate, and symbol resolution of all elements of the file at once, so the test setup is shared.
 */
abstract class AbstractResolveAllByPsiTest : AbstractResolveByElementTest() {
    override val resolveKinds: List<ResolveKind> get() = ResolveKind.entries

    override fun collectElementsToResolve(
        file: KtFile,
        module: KtTestModule,
        testServices: TestServices,
    ): Collection<ResolveTestCaseContext<KtElement>> = collectAllKtElements(file)
}
