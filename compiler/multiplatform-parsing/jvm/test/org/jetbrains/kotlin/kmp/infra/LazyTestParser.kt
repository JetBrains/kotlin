/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.platform.syntax.CancellationProvider
import com.intellij.platform.syntax.LazyParsingContext
import com.intellij.platform.syntax.SyntaxLanguage
import com.intellij.platform.syntax.canLazyNodeBeReparsedIncrementally
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import com.intellij.platform.syntax.lexer.TokenList
import com.intellij.platform.syntax.lexer.performLexing
import com.intellij.platform.syntax.parseLazyNode
import com.intellij.platform.syntax.parser.SyntaxTreeBuilderFactory
import com.intellij.platform.syntax.tree.KmpSyntaxNode
import com.intellij.platform.syntax.tree.SyntaxNode
import com.intellij.platform.syntax.tree.children
import com.intellij.platform.syntax.tree.defaultTokenizationPolicy
import com.intellij.platform.syntax.tree.parse
import com.intellij.platform.syntax.util.cancellation.CancellationProviderExtension
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.kmp.lexer.KotlinLexer
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.parser.KotlinParser
import org.jetbrains.kotlin.kmp.parser.KtNodeTypes
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds

/**
 * Builds [KmpSyntaxNode] trees with lazily parseable blocks and lambdas.
 * Converting to [TestParseNode] traverses the entire tree, so it forces parsing of all collapsed nodes.
 */
class LazyTestParser(disposable: Disposable) : AbstractTestParser<SyntaxNode>(ParseMode.NoKDoc) {
    init {
        // `KmpSyntaxNode` requests extensions from the IntelliJ application if it's in the classpath
        @OptIn(CoreEnvironmentDeprecation::class)
        KotlinCoreEnvironment.createForTests(disposable, CompilerConfiguration.create(), EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val extensionArea = ApplicationManager.getApplication().extensionArea
        if (!extensionArea.hasExtensionPoint(CANCELLATION_PROVIDER_EP_NAME)) {
            CoreApplicationEnvironment.registerApplicationExtensionPoint(
                ExtensionPointName(CANCELLATION_PROVIDER_EP_NAME),
                CancellationProviderExtension::class.java,
            )
        }
    }

    companion object {
        private const val CANCELLATION_PROVIDER_EP_NAME = "fleet.com.intellij.syntax.cancellationProviderExtension"

        private val kotlinLanguage = SyntaxLanguage("kotlin")

        private val noCancellation = CancellationProvider { }

        private val tokenizationPolicy = defaultTokenizationPolicy(logger = null)

        private val whitespaceOrCommentBindingPolicy = KotlinParser(isScript = false, isLazy = true).whitespaceOrCommentBindingPolicy

        private fun createBuilder(text: CharSequence, tokens: TokenList, startOffset: Int) =
            SyntaxTreeBuilderFactory.builder(text, tokens, KtTokens.WHITESPACES, KtTokens.COMMENTS)
                .withWhitespaceOrCommentBindingPolicy(whitespaceOrCommentBindingPolicy)
                .withStartOffset(startOffset)
                .build()
    }

    override fun parse(fileName: String, text: String): TestParseNode<out SyntaxNode> =
        parseToSyntaxNode(fileName, text).toTestParseNode()

    private fun parseToSyntaxNode(fileName: String, text: CharSequence): KmpSyntaxNode {
        val parser = KotlinParser(isScript(fileName), isLazy = true)
        return parse(
            text = text,
            lexerFactory = ::KotlinLexer,
            parser = parser::parse,
            whitespaces = KtTokens.WHITESPACES,
            comments = KtTokens.COMMENTS,
            documentLanguage = kotlinLanguage,
            languageProvider = { emptySequence() },
            cancellationProvider = noCancellation,
            tokenizationPolicy = tokenizationPolicy,
            whitespaceOrCommentBindingPolicy = parser.whitespaceOrCommentBindingPolicy,
        )
    }

    class ReparseResult(val reparsedNodeText: String, val newTree: TestSyntaxElement<SyntaxNode>)

    /**
     * Emulates incremental reparsing as it's performed by IDE:
     * finds the deepest lazy parseable node around the changed range that accepts the new text
     * and replaces it with the result of its parsing.
     *
     * @return `null` if no lazy parseable node accepts the new text, and the entire file should be reparsed.
     */
    fun tryReparse(fileName: String, oldText: String, newText: String): ReparseResult? {
        val changeStart = oldText.commonPrefixWith(newText).length
        val maxSuffixLength = minOf(oldText.length, newText.length) - changeStart
        val changeOldEnd = oldText.length - minOf(oldText.commonSuffixWith(newText).length, maxSuffixLength)
        val delta = newText.length - oldText.length

        val oldRoot = parseToSyntaxNode(fileName, oldText)
        val lazyNodes = generateSequence<SyntaxNode>(oldRoot) { node ->
            node.children().firstOrNull { it.startOffset <= changeStart && changeOldEnd <= it.endOffset && it.firstChild() != null }
        }.filter { it.type.isLazyParseable() }.toList()

        for (node in lazyNodes.asReversed()) {
            val newNodeText = newText.substring(node.startOffset, node.endOffset + delta)
            val newNodeTokens = performLexing(newNodeText, KotlinLexer(), noCancellation, logger = null)
            val parsingContext = LazyParsingContext(node, newNodeTokens, createBuilder(newNodeText, newNodeTokens, 0), noCancellation)
            if (!canLazyNodeBeReparsedIncrementally(parsingContext)) continue

            val reparsedNode = parse(
                text = newNodeText,
                lexerFactory = ::KotlinLexer,
                parser = { builder -> parseLazyNode(LazyParsingContext(node, builder.tokens, builder, noCancellation)) },
                whitespaces = KtTokens.WHITESPACES,
                comments = KtTokens.COMMENTS,
                documentLanguage = kotlinLanguage,
                languageProvider = { emptySequence() },
                cancellationProvider = noCancellation,
                tokenizationPolicy = tokenizationPolicy,
                whitespaceOrCommentBindingPolicy = whitespaceOrCommentBindingPolicy,
            ).toTestParseNode(shift = node.startOffset)

            return ReparseResult(
                newNodeText,
                oldRoot.toTestParseNode().substitute(node, reparsedNode, delta),
            )
        }

        return null
    }
}

/**
 * Replaces the node corresponding to the [target] with the [replacement] and shifts the following nodes by the [delta].
 */
private fun TestSyntaxElement<SyntaxNode>.substitute(
    target: SyntaxNode,
    replacement: TestParseNode<SyntaxNode>,
    delta: Int,
): TestParseNode<SyntaxNode> {
    return when {
        start == target.startOffset && end == target.endOffset && name == target.type.toString() -> replacement
        end <= target.startOffset -> shift(0)
        start >= target.endOffset -> shift(delta)
        else -> TestParseNode(name, start, end + delta, syntaxElement, children.map { it.substitute(target, replacement, delta) })
    }
}

private fun TestSyntaxElement<SyntaxNode>.shift(delta: Int): TestParseNode<SyntaxNode> =
    TestParseNode(name, start + delta, end + delta, syntaxElement, children.map { it.shift(delta) })

fun SyntaxNode.toTestParseNode(shift: Int = 0): TestParseNode<SyntaxNode> {
    return TestParseNode(
        type.toString(),
        startOffset + shift,
        endOffset + shift,
        this,
        // LightTree and PSI builders ignore empty leaf tokens by default (for instance, `DANGLING_NEWLINE`)
        children().filter { it.startOffset != it.endOffset || it.isMarker() }
            .map { it.toTestParseNode(shift) }
            .toList(),
    )
}

private fun SyntaxNode.isMarker(): Boolean =
    type == SyntaxTokenTypes.ERROR_ELEMENT || KtNodeTypes.getElementTypeId(type) != SyntaxElementTypesWithIds.NO_ID
