/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.renderFrontendIndependentKClassNameOf
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveTest<T> : AbstractAnalysisApiBasedTest() {
    protected abstract val resolveKind: String

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val elementsByMarker = collectElementsToResolve(mainFile, mainModule, testServices).groupBy { it.marker }
        val actual = prettyPrint {
            printMap(
                map = elementsByMarker,
                omitSingleKey = true,
                renderKey = { key, _ -> append("$key:") }
            ) { marker, byMarker ->
                val elementsByMarkerAndContext = byMarker.groupBy { it.context }
                printMap(
                    map = elementsByMarkerAndContext,
                    omitSingleKey = false,
                    renderKey = { key, value ->
                        append(key::class.simpleName)
                        if (key !is PsiFile) {
                            append(key.textRange.toString())
                        }

                        append(':')
                        val suffix = if (key is PsiFile) {
                            key.name
                        } else {
                            key.text.substringBefore('\n')
                        }

                        append(" '$suffix'")
                    }
                ) { context, byMarkerAndContext ->
                    printCollection(byMarkerAndContext, separator = "\n\n") { contextTestCase ->
                        val output = generateResolveOutput(contextTestCase, mainFile, mainModule, testServices)
                        val element = contextTestCase.element
                        if (element != null && element != contextTestCase.context) {
                            append(renderFrontendIndependentKClassNameOf(element))
                            appendLine(':')
                            withIndent {
                                append(output)
                            }
                        } else {
                            append(output)
                        }
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual, extension = "$resolveKind.txt")
        checkSuppressedExceptions(mainModule.testModule.directives, testServices)
    }

    protected fun <K, V> PrettyPrinter.printMap(
        map: Map<K, V>,
        omitSingleKey: Boolean,
        renderKey: PrettyPrinter.(K & Any, V) -> Unit,
        renderValue: PrettyPrinter.(K, V) -> Unit,
    ) {
        val entries = map.entries
        val renderKeyClass = entries.size > 1 || !omitSingleKey
        printCollection(entries, separator = "\n\n") { (key, value) ->
            if (renderKeyClass && key != null) {
                renderKey(key, value)
                appendLine()
                withIndent {
                    renderValue(key, value)
                }
            } else {
                renderValue(key, value)
            }
        }
    }

    protected abstract fun collectElementsToResolve(
        mainFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices,
    ): Collection<ResolveTestCaseContext<T>>

    protected abstract fun generateResolveOutput(
        context: ResolveTestCaseContext<T>,
        mainFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices,
    ): String

    interface ResolveTestCaseContext<V> {
        val element: V
        val context: KtElement?
        val marker: String?
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
        commonDirective = Directives.IGNORE_STABILITY,
        k1Directive = Directives.IGNORE_STABILITY_K1,
        k2Directive = Directives.IGNORE_STABILITY_K2,
    )

    private val suppressedStabilityExceptions: MutableList<Throwable> = mutableListOf()

    protected fun ignoreStabilityIfNeeded(body: () -> Unit): Unit = try {
        body()
    } catch (e: Throwable) {
        suppressedStabilityExceptions += e
    }

    private fun checkSuppressedExceptions(directives: RegisteredDirectives, testServices: TestServices) {
        val directive = directives.doNotCheckSymbolRestoreDirective()
        val isStabilitySuppressed = directive != null && directives[directive].let { values ->
            values.isEmpty() || values.any { it == resolveKind }
        }

        if (isStabilitySuppressed) {
            if (suppressedStabilityExceptions.isNotEmpty()) {
                return
            }

            error("Directive '${directive.name}' is not needed")
        }

        testServices.assertions.failAll(suppressedStabilityExceptions)
    }
}