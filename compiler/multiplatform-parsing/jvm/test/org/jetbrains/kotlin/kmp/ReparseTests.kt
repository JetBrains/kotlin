/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.kmp.infra.LazyTestParser
import org.jetbrains.kotlin.kmp.infra.NewTestParser
import org.jetbrains.kotlin.kmp.infra.ParseMode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Checks incremental reparsing of lazily parseable blocks and lambdas.
 *
 * `<caret>` marks the place where `before` is replaced with `after`.
 * The reparsed tree is compared with the result of the non-lazy parser on the new text.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReparseTests {
    private val disposable = Disposer.newDisposable("Disposable for `${javaClass.simpleName}`")
    private val lazyParser = LazyTestParser(disposable)
    private val nonLazyParser = NewTestParser(ParseMode.NoKDoc)

    @Test
    fun functionBodyStatementAdded() = checkReparsed(
        "fun f() { foo()<caret> }",
        before = "", after = "; bar()",
        expectedReparsedNode = "{ foo(); bar() }",
    )

    @Test
    fun functionBodyStatementChanged() = checkReparsed(
        "fun f() { val x = <caret>1 }",
        before = "1", after = "listOf(1).map { it * 2 }",
        expectedReparsedNode = "{ val x = listOf(1).map { it * 2 } }",
    )

    @Test
    fun tryBlock() = checkReparsed(
        "class C { fun f() { try { <caret> } finally {} } }",
        before = "", after = "foo()",
        expectedReparsedNode = "{ foo() }",
    )

    @Test
    fun initBlock() = checkReparsed(
        "class C { init { <caret> } }",
        before = "", after = "foo()",
        expectedReparsedNode = "{ foo() }",
    )

    @Test
    fun propertyAccessor() = checkReparsed(
        "val x: Int get() { return <caret>1 }",
        before = "1", after = "2 + 3",
        expectedReparsedNode = "{ return 2 + 3 }",
    )

    @Test
    fun lambdaBodyChanged() = checkReparsed(
        "val x = list.map { <caret>it }",
        before = "it", after = "it + 1",
        expectedReparsedNode = "{ it + 1 }",
    )

    @Test
    fun lambdaWithParametersBodyChanged() = checkReparsed(
        "val x = list.map { a, b -> <caret>a }",
        before = "a", after = "a + b",
        expectedReparsedNode = "{ a, b -> a + b }",
    )

    @Test
    fun lambdaInsideFunctionBody() = checkReparsed(
        "fun f() { list.map { a -> <caret>a } }",
        before = "a", after = "a + 1",
        expectedReparsedNode = "{ a -> a + 1 }",
    )

    @Test
    fun functionBodyUnbalancedBraceAdded() = checkNotReparsed(
        "fun f() { <caret> }\nfun g() {}",
        before = "", after = "{",
    )

    @Test
    fun functionBodyClosingBraceRemoved() = checkNotReparsed(
        "fun f() { foo() <caret>}\nfun g() {}",
        before = "}", after = "",
    )

    @Test
    fun functionBodyExtraClosingBraceAdded() = checkNotReparsed(
        "fun f() { foo() <caret>}\nfun g() {}",
        before = "", after = "} {",
    )

    // The `if` body can't be reparsed as a block because it becomes a lambda, so the enclosing function body is reparsed instead
    @Test
    fun blockBecomesLambdaInIf() = checkReparsed(
        "fun f() { if (c) {<caret> foo() } }",
        before = "", after = " a ->",
        expectedReparsedNode = "{ if (c) { a -> foo() } }",
    )

    @Test
    fun blockWithParenthesizedParametersBecomesLambdaInIf() = checkReparsed(
        "fun f() { if (c) {<caret> foo() } }",
        before = "", after = " (a, b) ->",
        expectedReparsedNode = "{ if (c) { (a, b) -> foo() } }",
    )

    @Test
    fun blockWithBracketedParametersBecomesLambdaInIf() = checkReparsed(
        "fun f() { if (c) {<caret> foo() } }",
        before = "", after = " [a, b] ->",
        expectedReparsedNode = "{ if (c) { [a, b] -> foo() } }",
    )

    @Test
    fun blockInIfChanged() = checkReparsed(
        "fun f() { if (c) { <caret>foo() } }",
        before = "foo", after = "bar",
        expectedReparsedNode = "{ bar() }",
    )

    @Test
    fun lambdaArrowRemovedInIf() = checkReparsed(
        "fun f() { if (c) { a<caret> -> foo() } }",
        before = " ->", after = "",
        expectedReparsedNode = "{ if (c) { a foo() } }",
    )

    @Test
    fun lambdaParameterCommaRemovedInIf() = checkReparsed(
        "fun f() { if (c) { a<caret>, b -> foo() } }",
        before = ",", after = "",
        expectedReparsedNode = "{ if (c) { a b -> foo() } }",
    )

    @Test
    fun lambdaArrowMovedInsideFunctionBody() = checkReparsed(
        "fun f() { list.map { a<caret> -> a } }",
        before = " -> a", after = " a ->",
        expectedReparsedNode = "{ list.map { a a -> } }",
    )

    @Test
    fun topLevelBlockBecomesLambdaInIf() = checkNotReparsed(
        "val x = if (c) {<caret> foo() } else null",
        before = "", after = " a ->",
    )

    @Test
    fun topLevelLambdaArrowMoved() = checkNotReparsed(
        "val x = list.map { a<caret> -> a }",
        before = " -> a", after = " a ->",
    )

    @Test
    fun topLevelLambdaParameterCommaRemoved() = checkNotReparsed(
        "val x = list.map { a<caret>, b -> a }",
        before = ",", after = "",
    )

    private fun checkReparsed(textWithCaret: String, before: String, after: String, expectedReparsedNode: String) {
        val [oldText, newText] = applyChange(textWithCaret, before, after)
        val result = lazyParser.tryReparse("test.kt", oldText, newText)
        assertNotNull(result, "Expected incremental reparse to succeed")
        assertEquals(expectedReparsedNode, result.reparsedNodeText)

        assertEquals(
            nonLazyParser.parse("test.kt", newText).dump(sourceLinesMapping = null, text = newText),
            result.newTree.dump(sourceLinesMapping = null, text = newText),
        )
    }

    private fun checkNotReparsed(textWithCaret: String, before: String, after: String) {
        val [oldText, newText] = applyChange(textWithCaret, before, after)
        val result = lazyParser.tryReparse("test.kt", oldText, newText)
        assertNull(result, "Expected incremental reparse to be rejected, but `${result?.reparsedNodeText}` was reparsed")
    }

    private fun applyChange(textWithCaret: String, before: String, after: String): Pair<String, String> {
        val caret = textWithCaret.indexOf(CARET)
        val prefix = textWithCaret.substring(0, caret)
        val suffix = textWithCaret.substring(caret + CARET.length)
        require(suffix.startsWith(before)) { "Text after caret should start with `$before`" }
        return (prefix + suffix) to (prefix + after + suffix.substring(before.length))
    }

    @AfterAll
    fun cleanup() {
        disposeRootInWriteAction(disposable)
    }

    private companion object {
        const val CARET = "<caret>"
    }
}
