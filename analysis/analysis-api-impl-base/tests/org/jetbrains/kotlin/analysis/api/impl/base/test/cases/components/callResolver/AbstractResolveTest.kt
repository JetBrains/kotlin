/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractResolveTest.Directives.IGNORE_STABILITY
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractResolveTest.Directives.IGNORE_STABILITY_K1
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractResolveTest.Directives.IGNORE_STABILITY_K2
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveTest : AbstractAnalysisApiBasedTest() {
    protected abstract val resolveKind: String

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val elementsToProcess = elementsToProcess(mainFile, mainModule, testServices)
        val actual = if (elementsToProcess.size == 1) {
            val mainElement = elementsToProcess.single().elementToProcess
            "${mainElement::class.simpleName}:\n" + generateResolveOutput(mainElement, testServices)
        } else {
            elementsToProcess.joinToString("\n\n") { (element, marker) ->
                val output = generateResolveOutput(element, testServices)
                "$marker: ${element::class.simpleName}:\n$output"
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual, extension = "$resolveKind.txt")
    }

    private data class TestContext(val elementToProcess: KtElement, val marker: String)

    private fun elementsToProcess(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices): List<TestContext> {
        val carets = testServices.expressionMarkerProvider.getAllCarets(mainFile)
        if (carets.size > 1) {
            return carets.map {
                val element = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtElement>(mainFile, it.tag)
                TestContext(element, it.fullTag)
            }
        }

        val expression = testServices.expressionMarkerProvider.getElementOfTypeAtCaretOrNull<KtExpression>(mainFile)
            ?: testServices.expressionMarkerProvider.getSelectedElementOfTypeByDirective(
                ktFile = mainFile,
                module = mainModule,
                defaultType = KtElement::class,
            ) as KtElement

        return listOf(TestContext(expression.elementToResolve, "<caret>"))
    }

    abstract fun generateResolveOutput(mainElement: KtElement, testServices: TestServices): String

    protected val KtElement.elementToResolve: KtElement
        get() = when (this) {
            is KtValueArgument -> getArgumentExpression()!!
            is KtDeclarationModifierList -> annotationEntries.singleOrNull() ?: error("Only single annotation entry is supported for now")
            is KtFileAnnotationList -> annotationEntries.singleOrNull() ?: error("Only single annotation entry is supported for now")
            else -> this
        }

    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_STABILITY by stringDirective(
            description = "Symbol restoring for some symbols in current test is not supported yet",
        )

        val IGNORE_STABILITY_K1 by stringDirective(
            description = "Symbol restoring for some symbols in current test is not supported yet in K1",
        )

        val IGNORE_STABILITY_K2 by stringDirective(
            description = "Symbol restoring for some symbols in current test is not supported yet in K2",
        )
    }

    protected fun RegisteredDirectives.doNotCheckSymbolRestoreDirective(): StringDirective? = findSpecificDirective(
        commonDirective = IGNORE_STABILITY,
        k1Directive = IGNORE_STABILITY_K1,
        k2Directive = IGNORE_STABILITY_K2,
    )

    protected fun ignoreStabilityIfNeeded(directives: RegisteredDirectives, body: () -> Unit) {
        val directive = directives.doNotCheckSymbolRestoreDirective()
        val isStabilitySuppressed = directive != null && directives[directive].let { values ->
            values.isEmpty() || values.any { it == resolveKind }
        }

        try {
            body()
        } catch (e: Throwable) {
            if (isStabilitySuppressed) return
            throw e
        }

        if (isStabilitySuppressed) {
            error("Directive '${directive.name}' is not needed")
        }
    }
}