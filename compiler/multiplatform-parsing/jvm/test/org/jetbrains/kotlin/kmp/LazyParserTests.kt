/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import com.intellij.openapi.util.Disposer
import com.intellij.platform.syntax.tree.SyntaxNode
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.kmp.infra.LazyTestParser
import org.jetbrains.kotlin.kmp.infra.NewParserTestNode
import org.jetbrains.kotlin.kmp.infra.NewTestParser
import org.jetbrains.kotlin.kmp.infra.ParseMode
import org.jetbrains.kotlin.kmp.infra.TestParseNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance

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

    @AfterAll
    fun cleanup() {
        disposeRootInWriteAction(disposable)
    }
}
