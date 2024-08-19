/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.renderFrontendIndependentKClassNameOf
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.ComposedRegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * The test supports multiple modules each with a main file. When we have multiple test files in total, the test output contains a separate
 * section for each file/module pair, with the output for each file indented under a header. For a single test file, neither a header nor
 * the indentation are generated.
 */
abstract class AbstractResolveTest<T> : AbstractAnalysisApiBasedTest() {
    protected abstract val resolveKind: String

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    override fun doTest(testServices: TestServices) {
        val modules = findMainModule(testServices)?.let { listOf(it) }
            ?: testServices.ktTestModuleStructure.mainModules

        val fileResolutionTargets = modules.mapNotNull { module ->
            // Only modules with a main file are considered resolution targets. Due to the logic of `findMainFile`, we cannot currently have
            // multiple files with carets/selections in the same module. All such files would be main files, but the test infrastructure
            // expects a single main file per module. So it's currently not practical to have multiple target files per module, but this can
            // be changed if the need arises. Additional files in a module are supported, but they cannot contain carets/selections and
            // won't be considered as resolution targets.
            val mainFile = findMainFile(module, testServices) ?: return@mapNotNull null
            val targetElements = collectElementsToResolve(mainFile, module, testServices)

            FileResolutionTarget(
                targetElements,
                mainFile,
                module,
            )
        }

        require(fileResolutionTargets.isNotEmpty()) { "Expected at least one target file with at least one target element." }

        val actual = prettyPrint {
            if (fileResolutionTargets.size == 1) {
                printFileResolutionOutput(fileResolutionTargets.single(), testServices)
            } else {
                printCollection(fileResolutionTargets, separator = "\n\n") { target ->
                    appendLine("${target.module.testModule.name} - ${target.file.name}:")
                    withIndent {
                        printFileResolutionOutput(target, testServices)
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual, extension = "$resolveKind.txt")

        val lastTestModule = modules.last().testModule
        val directives = ComposedRegisteredDirectives(
            // Use the last file to avoid offsets changes
            lastTestModule.files.last().directives,

            // Use module directives as well to support the case with implicit structure
            lastTestModule.directives,
        )

        checkSuppressedExceptions(directives, testServices)
    }

    private class FileResolutionTarget<T>(
        val targetElements: Collection<ResolveTestCaseContext<T>>,
        val file: KtFile,
        val module: KtTestModule,
    )

    private fun PrettyPrinter.printFileResolutionOutput(fileResolutionTarget: FileResolutionTarget<T>, testServices: TestServices) {
        val elementsByMarker = fileResolutionTarget.targetElements.groupBy { it.marker }

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
                    val output = generateResolveOutput(
                        contextTestCase,
                        fileResolutionTarget.file,
                        fileResolutionTarget.module,
                        testServices
                    )

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
        file: KtFile,
        module: KtTestModule,
        testServices: TestServices,
    ): Collection<ResolveTestCaseContext<T>>

    protected abstract fun generateResolveOutput(
        context: ResolveTestCaseContext<T>,
        file: KtFile,
        module: KtTestModule,
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
            applicability = DirectiveApplicability.Any,
        )

        val IGNORE_STABILITY_K1 by stringDirective(
            description = "Symbol restoring for some symbols in current test is not supported yet in K1",
            applicability = DirectiveApplicability.Any,
        )

        val IGNORE_STABILITY_K2 by stringDirective(
            description = "Symbol restoring for some symbols in current test is not supported yet in K2",
            applicability = DirectiveApplicability.Any,
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