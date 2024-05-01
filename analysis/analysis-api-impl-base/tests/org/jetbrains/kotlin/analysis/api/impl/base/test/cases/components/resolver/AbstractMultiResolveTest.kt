/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver.AbstractResolveTest.Companion.asKtElement
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import kotlin.reflect.KClass

abstract class AbstractMultiResolveTest : AbstractResolveTest() {
    override fun doTest(testServices: TestServices) {
        val printer = ResolvePrinter()

        process(testServices) { element ->
            analyseForTest(element.asKtElement) {
                processElement(
                    element = element,
                    testServices = testServices,
                    renderedSymbol = { printer.appendSymbol(element, it) },
                    renderedCall = { printer.appendCall(element, it) },
                    renderedCandidates = { printer.appendCandidates(element, it) },
                )
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(printer.symbolsOutput(), "symbols.txt")
        if (withCalls) {
            testServices.assertions.assertEqualsToTestDataFileSibling(printer.callsOutput(), "calls.txt")
            testServices.assertions.assertEqualsToTestDataFileSibling(printer.candidatesOutput(), "candidates.txt")
        }
    }

    /**
     * @param consumer accept applicable elements (see [asKtElement])
     */
    protected abstract fun process(testServices: TestServices, consumer: (Any) -> Unit)

    protected open val withCalls: Boolean get() = true
}

private sealed class ResolveResult(val context: Any, val rendered: String) {
    class Symbol(context: Any, rendered: String) : ResolveResult(context, rendered)
    class Call(context: Any, rendered: String) : ResolveResult(context, rendered)
    class Candidate(context: Any, rendered: String) : ResolveResult(context, rendered)

    fun containingFile(): PsiFile = context.asKtElement.containingFile
}

private class ResolvePrinter {
    private val result = mutableListOf<ResolveResult>()

    private inline fun <reified T : ResolveResult> output(): String {
        val elements = result.filterIsInstance<T>().groupBy { it.containingFile() }
        if (elements.isEmpty()) return ""

        return prettyPrint {
            val hasMoreThanOneFile = elements.keys.size > 1
            for ((file, results) in elements) {
                withFileNameIfNeeded(file, hasMoreThanOneFile) {
                    for (result in results) {
                        withContext(result.context) {
                            appendLine(result.rendered)
                        }
                    }
                }
            }
        }
    }

    fun appendSymbol(context: Any, symbol: String) {
        result += ResolveResult.Symbol(context, symbol)
    }

    fun symbolsOutput(): String = output<ResolveResult.Symbol>()

    fun appendCall(context: Any, call: String) {
        result += ResolveResult.Call(context, call)
    }

    fun callsOutput(): String = output<ResolveResult.Call>()

    fun appendCandidates(context: Any, candidates: String) {
        result += ResolveResult.Candidate(context, candidates)
    }

    fun candidatesOutput(): String = output<ResolveResult.Candidate>()

    private fun PrettyPrinter.withContext(context: Any, body: PrettyPrinter.() -> Unit) {
        renderCommonClassName(context::class)
        val psiElementToRender = when (context) {
            is KtElement -> context
            is KtReference -> {
                context.element.also {
                    append('|')
                    renderCommonClassName(it::class)
                }
            }
            else -> error("Unsupported type")
        }

        append(psiElementToRender.textRange.toString())
        appendLine(": '${psiElementToRender.text.substringBefore('\n')}'")
        withIndent {
            body()
        }

        appendLine()
    }

}

private fun PrettyPrinter.renderCommonClassName(clazz: KClass<*>) {
    var classToRender = clazz.java
    while (classToRender.simpleName.let { it.contains("Fir") || it.contains("Fe10") } == true) {
        classToRender = classToRender.superclass
    }

    append(classToRender.simpleName)
}

private fun PrettyPrinter.withFileNameIfNeeded(file: PsiFile, withFileName: Boolean, block: PrettyPrinter.() -> Unit) {
    if (withFileName) {
        appendLine(file.name)
        withIndent(block)
    } else {
        block()
    }
}