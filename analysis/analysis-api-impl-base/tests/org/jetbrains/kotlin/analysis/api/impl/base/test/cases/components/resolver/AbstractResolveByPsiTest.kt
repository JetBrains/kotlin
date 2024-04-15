/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * This test case is supposed to dump all resolution information such as symbols, calls, and call candidates from a file.
 */
abstract class AbstractResolveByPsiTest : AbstractResolveTest() {
    override fun doTest(testServices: TestServices) {
        val printer = ResolvePrinter()

        val filesToProcess = testServices.ktTestModuleStructure
            .mainModules
            .filter {
                when (it.moduleKind) {
                    TestModuleKind.Source, TestModuleKind.ScriptSource, TestModuleKind.LibrarySource, TestModuleKind.CodeFragment -> true
                    else -> false
                }
            }
            .flatMap { it.ktFiles }

        if (filesToProcess.isEmpty()) error("Where are no files to process")
        val renderFileNames = filesToProcess.size > 1

        for (file in filesToProcess) {
            printer.withFileNameIfNeeded(file, renderFileNames) {
                processFile(file, printer, testServices)
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(printer.symbolsOutput(), "symbols.txt")
        testServices.assertions.assertEqualsToTestDataFileSibling(printer.callsOutput(), "calls.txt")
        testServices.assertions.assertEqualsToTestDataFileSibling(printer.candidatesOutput(), "candidates.txt")
    }

    private fun processFile(file: KtFile, printer: ResolvePrinter, testServices: TestServices) {
        file.forEachDescendantOfType<KtElement> { element ->
            processElement(
                element = element,
                testServices = testServices,
                renderedSymbol = { printer.appendSymbol(element, it) },
                renderedCall = { printer.appendCall(element, it) },
                renderedCandidates = { printer.appendCandidates(element, it) },
            )
        }
    }
}

private class ResolvePrinter {
    private val symbolsBuilder = PrettyPrinter()
    private val callsBuilder = PrettyPrinter()
    private val candidatesBuilder = PrettyPrinter()

    fun withFileNameIfNeeded(file: KtFile, withFileName: Boolean, block: ResolvePrinter.() -> Unit) {
        symbolsBuilder.withFileNameIfNeeded(file, withFileName) {
            callsBuilder.withFileNameIfNeeded(file, withFileName) {
                candidatesBuilder.withFileNameIfNeeded(file, withFileName) {
                    block()
                }
            }
        }
    }

    fun appendSymbol(context: KtElement, symbol: String) {
        symbolsBuilder.withContext(context) {
            appendLine(symbol)
        }
    }

    fun symbolsOutput(): String = symbolsBuilder.toString()

    fun appendCall(context: KtElement, call: String) {
        callsBuilder.withContext(context) {
            appendLine(call)
        }
    }

    fun callsOutput(): String = callsBuilder.toString()

    fun appendCandidates(context: KtElement, candidates: String) {
        candidatesBuilder.withContext(context) {
            appendLine(candidates)
        }
    }

    fun candidatesOutput(): String = candidatesBuilder.toString()

    private fun PrettyPrinter.withContext(context: KtElement, body: PrettyPrinter.() -> Unit) {
        append(context::class.simpleName)
        append(context.textRange.toString())
        appendLine(": '${context.text.substringBefore('\n')}'")
        withIndent {
            body()
        }

        appendLine()
    }
}

private fun PrettyPrinter.withFileNameIfNeeded(file: KtFile, withFileName: Boolean, block: PrettyPrinter.() -> Unit) {
    if (withFileName) {
        appendLine(file.name)
        withIndent(block)
    } else {
        block()
    }
}