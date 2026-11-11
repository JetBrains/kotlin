/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import com.intellij.openapi.util.Disposer
import com.intellij.platform.syntax.tree.SyntaxNode
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.codeMetaInfo.clearTextFromDiagnosticMarkup
import org.jetbrains.kotlin.kmp.infra.LazyTestParser
import org.jetbrains.kotlin.kmp.infra.NewParserTestNode
import org.jetbrains.kotlin.kmp.infra.NewTestParser
import org.jetbrains.kotlin.kmp.infra.ParseMode
import org.jetbrains.kotlin.kmp.infra.TestParseNode
import org.jetbrains.kotlin.kmp.infra.checkSyntaxElements
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertTrue

/**
 * Checks that lazily parsed blocks and lambdas after expansion produce the same tree as the non-lazy parser.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LazyParserTests : AbstractRecognizerTests<
        NewParserTestNode,
        SyntaxNode,
        TestParseNode<out NewParserTestNode>,
        TestParseNode<out SyntaxNode>
        >() {
    private val disposable = Disposer.newDisposable("Disposable for `${javaClass.simpleName}`")
    private val nonLazyParser = NewTestParser(ParseMode.NoKDoc)
    private val lazyParser = LazyTestParser(disposable)

    override fun recognizeOldSyntaxElement(fileName: String, text: String): TestParseNode<out NewParserTestNode> =
        nonLazyParser.parse(fileName, text)

    override fun recognizeNewSyntaxElement(fileName: String, text: String): TestParseNode<out SyntaxNode> =
        lazyParser.parse(fileName, text)

    override val recognizerName: String = "parser"

    override val oldRecognizerSuffix: String = " (non-lazy)"

    override val recognizerSyntaxElementName: String = "parse node"

    override val ignoreFilesWithSyntaxError: Boolean = true

    /**
     * Lazy parsing and expanding of deeply nested lambdas and function bodies should not depend on the nesting depth (KTI-3876).
     * The non-lazy parser and the tree comparison are recursive, so they run on a thread with a large stack.
     */
    @Test
    fun testDeeplyNestedLambdasAndFunctionBodies() {
        val depth = 2000
        val text = "fun f() {\n" + "run {\nfun g() {\n".repeat(depth) + "}\n}\n".repeat(depth) + "}\n"

        val lazyTree = lazyParser.parse("test.kt", text)

        val areStructurallyEqual = runWithStack(LARGE_STACK_SIZE) {
            checkSyntaxElements(nonLazyParser.parse("test.kt", text), lazyTree)
        }
        assertTrue(areStructurallyEqual, "Lazy parser produced a different tree for deeply nested code")
    }

    /**
     * The deepest tree among the test data files is a long chain of binary operators inside a lazily parsed function body.
     * Its lazy parsing and expanding should work with a stack much smaller than the default one (KTI-3876).
     */
    @Test
    fun testLongBinaryExpressionChainWithSmallStack() {
        val file = testDataDirs
            .map { it.resolve("diagnostics/tests/expresssions/operators/binaryOpStackOverflow.kt") }
            .single { it.exists() }
        val text = clearTextFromDiagnosticMarkup(file.readText())

        val lazyTree = runWithStack(SMALL_STACK_SIZE) { lazyParser.parse(file.name, text) }

        assertTrue(
            checkSyntaxElements(nonLazyParser.parse(file.name, text), lazyTree),
            "Lazy parser produced a different tree for `${file.name}`"
        )
    }

    private fun <T> runWithStack(stackSize: Long, block: () -> T): T {
        var result: Result<T>? = null
        val thread = Thread(null, { result = runCatching(block) }, "Thread with $stackSize bytes of stack", stackSize)
        thread.start()
        thread.join()
        return result!!.getOrThrow()
    }

    @AfterAll
    fun cleanup() {
        disposeRootInWriteAction(disposable)
    }

    private companion object {
        /**
         * The non-lazy parsing and the comparison of the trees in [testDeeplyNestedLambdasAndFunctionBodies] need 4–8 MiB of stack.
         */
        const val LARGE_STACK_SIZE = 64L shl 20

        /**
         * A quarter of the default thread stack size. The recursive conversion of lazy trees overflowed even the default one.
         */
        const val SMALL_STACK_SIZE = 256L shl 10
    }
}
