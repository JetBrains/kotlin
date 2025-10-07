/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kdoc.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag

open class KDocTag(node: ASTNode) : KDocElementImpl(node) {

    /**
     * Returns the name of this tag, not including the leading @ character.
     *
     * @return tag name or null if this tag represents the default section of a doc comment
     * or the code has a syntax error.
     */
    override fun getName(): String? {
        val tagName: PsiElement? = findChildByType(KDocTokens.TAG_NAME)
        if (tagName != null) {
            return tagName.text.substring(1)
        }
        return null
    }

    /**
     * Returns the name of the entity documented by this tag (for example, the name of the parameter
     * for the @param tag), or null if this tag does not document any specific entity.
     */
    open fun getSubjectName(): String? = getSubjectLink()?.getLinkText()

    fun getSubjectLink(): KDocLink? {
        val children = childrenAfterTagName()
        if (hasSubject(children)) {
            return children.firstOrNull()?.psi as? KDocLink
        }
        return null
    }

    val knownTag: KDocKnownTag?
        get() {
            return name?.let { KDocKnownTag.findByTagName(it) }
        }

    private fun hasSubject(contentChildren: List<ASTNode>): Boolean {
        if (knownTag?.isReferenceRequired ?: false) {
            return contentChildren.firstOrNull()?.elementType == KDocTokens.MARKDOWN_LINK
        }
        return false
    }

    private fun childrenAfterTagName(): List<ASTNode> =
        node.getChildren(null)
            .dropWhile { it.elementType == KDocTokens.TAG_NAME }
            .dropWhile { it.elementType == TokenType.WHITE_SPACE }

    /**
     * Returns the content of this tag (all text following the tag name and the subject if present,
     * with leading asterisks removed).
     */
    open fun getContent(): String {
        val builder = StringBuilder()
        val codeBlockBuilder = StringBuilder()
        var targetBuilder = builder

        var contentStarted = false
        var afterAsterisk = false
        var indentedCodeBlock = false

        fun isCodeBlock() = targetBuilder == codeBlockBuilder

        fun startCodeBlock() {
            targetBuilder = codeBlockBuilder
        }

        fun flushCodeBlock() {
            if (isCodeBlock()) {
                builder.append(trimCommonIndent(codeBlockBuilder, indentedCodeBlock))
                codeBlockBuilder.setLength(0)
                targetBuilder = builder
            }
        }

        var children = childrenAfterTagName()
        if (hasSubject(children)) {
            children = children.drop(1)
        }
        for (node in children) {
            val type = node.elementType
            val nodeText = node.text
            val isTextIndented = nodeText.isIndented()

            if (type == KDocTokens.CODE_BLOCK_TEXT) {
                /**
                 * We have to check whether every single line of the current code block is indented.
                 * Checking just the first line is not enough since the following case would be considered indented:
                 * ```
                 *     line 1
                 *    line 2
                 *   line 3
                 *  line 4
                 * ```
                 */
                indentedCodeBlock = (!isCodeBlock() || indentedCodeBlock) && isTextIndented
                startCodeBlock()
            } else if (KDocTokens.CONTENT_TOKENS.contains(type)) {
                flushCodeBlock()
                indentedCodeBlock = false
            }

            if (KDocTokens.CONTENT_TOKENS.contains(type)) {
                /**
                 * One line can contain multiple content tokens.
                 * `afterAsterisk` indicates that the current token is the first one on the line, so we don't trim middle tokens.
                 * However, when text is placed right after the KDoc start (after `/`**),
                 * there is no leading asterisk.
                 * That's why we also need to check whether the content hasn't started yet, and this is the first content token.
                 *
                 * Text in code blocks is handled separately in [trimCommonIndent].
                 * If the text is indented and not a code block, we shouldn't trim it, as it might be a nested list.
                 * ```
                 * - Line1
                 *      - Line2
                 * ```
                 */
                val trimLeadingSpaces = (!contentStarted || afterAsterisk) && !(isCodeBlock() || isTextIndented)

                targetBuilder.append(if (trimLeadingSpaces) nodeText.trimStart() else nodeText)
                contentStarted = true
                afterAsterisk = false
            }
            if (type == KDocTokens.LEADING_ASTERISK) {
                afterAsterisk = true
            }
            if (type == TokenType.WHITE_SPACE && contentStarted) {
                targetBuilder.append("\n".repeat(StringUtil.countNewLines(nodeText)))
            }
            if (type == KDocElementTypes.KDOC_TAG) {
                break
            }
        }

        flushCodeBlock()

        return builder.toString().trimEnd(' ', '\t')
    }

    private fun trimCommonIndent(builder: StringBuilder, prepend4WhiteSpaces: Boolean = false): String {
        val lines = builder.lines()
        val minIndent = lines.filter { it.isNotBlank() }.minOfOrNull { it.calcIndent() } ?: 0

        val processedLines = lines.map { line ->
            if (line.isNotBlank()) {
                line.drop(minIndent).let { if (prepend4WhiteSpaces) it.prependIndent(indentationWhiteSpaces) else it }
            } else {
                ""
            }
        }
        return processedLines.joinToString("\n")
    }

    private fun String.calcIndent() = indexOfFirst { !it.isWhitespace() }
    private fun String.isIndented() = startsWith(indentationWhiteSpaces) || startsWith("\t")

    companion object {
        val indentationWhiteSpaces = " ".repeat(4)
    }
}
