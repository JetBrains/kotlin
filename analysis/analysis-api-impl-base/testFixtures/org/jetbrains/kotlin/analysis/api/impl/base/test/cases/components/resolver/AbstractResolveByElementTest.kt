/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfTypeInPreorder
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractResolveByElementTest : AbstractResolveTest<KtElement>() {
    final override fun generateResolveOutput(
        context: ResolveTestCaseContext<KtElement>,
        file: KtFile,
        module: KtTestModule,
        testServices: TestServices,
    ): String = generateResolveOutput(context.element, testServices)

    abstract fun generateResolveOutput(mainElement: KtElement, testServices: TestServices): String

    override fun collectElementsToResolve(
        file: KtFile,
        module: KtTestModule,
        testServices: TestServices,
    ): Collection<ResolveTestCaseContext<KtElement>> {
        val carets = testServices.expressionMarkerProvider.getAllCarets(file)
        if (carets.size > 1) {
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
}