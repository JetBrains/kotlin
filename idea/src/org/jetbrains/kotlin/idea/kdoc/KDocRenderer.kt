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

package org.jetbrains.kotlin.idea.kdoc

import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.lang.documentation.DocumentationMarkup.*
import com.intellij.psi.PsiElement
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.wrapTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

object KDocRenderer {
    fun renderKDoc(docComment: KDocTag): String {
        return if (docComment is KDocSection) {
            renderKDocContent(docComment) + renderKDocSection(docComment)
        } else {
            renderKDocContent(docComment)
        }
    }

    fun renderKDocContent(docComment: KDocTag): String {
        return markdownToHtml(docComment.getContent(), allowSingleParagraph = true)
    }

    fun StringBuilder.appendKDocContent(docComment: KDocTag) {
        append(renderKDocContent(docComment))
    }


    fun renderKDocSection(section: KDocSection): String = buildString {
        appendKDocSection(section)
    }

    fun StringBuilder.appendKDocSection(section: KDocSection) {
        renderTag(section.findTagByName("receiver"), "Receiver", this)
        val paramTags = section.findTagsByName("param").filter { it.getSubjectName() != null }
        renderTagList(paramTags, "Params", this)

        renderTag(section.findTagByName("return"), "Returns", this)

        val throwsTags = (section.findTagsByName("exception").union(section.findTagsByName("throws")))
            .filter { it.getSubjectName() != null }
        renderTagList(throwsTags, "Throws", this)

        renderTag(section.findTagByName("author"), "Author", this)
        renderTag(section.findTagByName("since"), "Since", this)

        renderSeeAlso(section, this)

        val sampleTags = section.findTagsByName("sample").filter { it.getSubjectLink() != null }
        renderSamplesList(sampleTags, this)
    }

    private fun KDocLink.createHyperlink(to: StringBuilder) {
        DocumentationManagerUtil.createHyperlink(to, getLinkText(), getLinkText(), false)
    }

    private fun KDocLink.getTargetElement(): PsiElement? {
        return this.getChildrenOfType<KDocName>().last().mainReference.resolve()
    }

    private fun PsiElement.extractExampleText() = when (this) {
        is KtDeclarationWithBody -> {
            val bodyExpression = bodyExpression
            when (bodyExpression) {
                is KtBlockExpression -> bodyExpression.text.removeSurrounding("{", "}")
                else -> bodyExpression!!.text
            }
        }
        else -> text
    }

    private fun trimCommonIndent(text: String): String {
        fun String.leadingIndent() = indexOfFirst { !it.isWhitespace() }

        val lines = text.split('\n')
        val minIndent = lines.filter { it.trim().isNotEmpty() }.map(String::leadingIndent).min() ?: 0
        return lines.joinToString("\n") { it.drop(minIndent) }
    }

    private fun StringBuilder.renderSection(title: String, content: StringBuilder.() -> Unit) {
        append(SECTION_HEADER_START, title, ":", SECTION_SEPARATOR)
        content()
        append(SECTION_END)
    }

    private fun renderSamplesList(sampleTags: List<KDocTag>, to: StringBuilder) {
        if (sampleTags.isEmpty()) return

        to.renderSection("Samples") {
            sampleTags.forEach {
                it.getSubjectLink()?.let { subjectLink ->
                    append("<p>")
                    subjectLink.createHyperlink(to)
                    val target = subjectLink.getTargetElement()
                    wrapTag("pre") {
                        wrapTag("code") {
                            if (target == null)
                                to.append("// Unresolved")
                            else {
                                to.append(trimCommonIndent(target.extractExampleText()).htmlEscape())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderSeeAlso(docComment: KDocSection, to: StringBuilder) {
        val seeTags = docComment.findTagsByName("see")
        if (seeTags.isEmpty()) return

        to.renderSection("See Also") {
            seeTags.forEachIndexed { index, tag ->
                val subjectName = tag.getSubjectName()
                if (subjectName != null) {
                    DocumentationManagerUtil.createHyperlink(this, subjectName, subjectName, false)
                } else {
                    append(tag.getContent())
                }
                if (index < seeTags.size - 1) {
                    append(", ")
                }
            }
        }
    }

    private fun renderTagList(tags: List<KDocTag>, title: String, to: StringBuilder) {
        if (tags.isEmpty()) {
            return
        }
        to.renderSection(title) {
            tags.forEach {
                append("<p><code>${it.getSubjectName()}</code> - ${markdownToHtml(it.getContent().trimStart())}")
            }
        }
    }

    private fun renderTag(tag: KDocTag?, title: String, to: StringBuilder) {
        if (tag != null) {
            to.renderSection(title) {
                append(markdownToHtml(tag.getContent()))
            }
        }
    }

    fun markdownToHtml(markdown: String, allowSingleParagraph: Boolean = false): String {
        val markdownTree = MarkdownParser(CommonMarkFlavourDescriptor()).buildMarkdownTreeFromString(markdown)
        val markdownNode = MarkdownNode(markdownTree, null, markdown)

        // Avoid wrapping the entire converted contents in a <p> tag if it's just a single paragraph
        val maybeSingleParagraph = markdownNode.children.singleOrNull { it.type != MarkdownTokenTypes.EOL }
        return if (maybeSingleParagraph != null && !allowSingleParagraph) {
            maybeSingleParagraph.children.joinToString("") {
                if (it.text == "\n") " " else it.toHtml()
            }
        } else {
            markdownNode.toHtml()
        }
    }

    class MarkdownNode(val node: ASTNode, val parent: MarkdownNode?, val markdown: String) {
        val children: List<MarkdownNode> = node.children.map { MarkdownNode(it, this, markdown) }
        val endOffset: Int get() = node.endOffset
        val startOffset: Int get() = node.startOffset
        val type: IElementType get() = node.type
        val text: String get() = markdown.substring(startOffset, endOffset)
        fun child(type: IElementType): MarkdownNode? = children.firstOrNull { it.type == type }
    }

    private fun MarkdownNode.visit(action: (MarkdownNode, () -> Unit) -> Unit) {
        action(this) {
            for (child in children) {
                child.visit(action)
            }
        }
    }

    private fun MarkdownNode.toHtml(): String {
        if (node.type == MarkdownTokenTypes.WHITE_SPACE) {
            return text   // do not trim trailing whitespace
        }

        val sb = StringBuilder()
        visit { node, processChildren ->
            fun wrapChildren(tag: String, newline: Boolean = false) {
                sb.append("<$tag>")
                processChildren()
                sb.append("</$tag>")
                if (newline) sb.appendln()
            }

            val nodeType = node.type
            val nodeText = node.text
            when (nodeType) {
                MarkdownElementTypes.UNORDERED_LIST -> wrapChildren("ul", newline = true)
                MarkdownElementTypes.ORDERED_LIST -> wrapChildren("ol", newline = true)
                MarkdownElementTypes.LIST_ITEM -> wrapChildren("li")
                MarkdownElementTypes.EMPH -> wrapChildren("em")
                MarkdownElementTypes.STRONG -> wrapChildren("strong")
                MarkdownElementTypes.ATX_1 -> wrapChildren("h1")
                MarkdownElementTypes.ATX_2 -> wrapChildren("h2")
                MarkdownElementTypes.ATX_3 -> wrapChildren("h3")
                MarkdownElementTypes.ATX_4 -> wrapChildren("h4")
                MarkdownElementTypes.ATX_5 -> wrapChildren("h5")
                MarkdownElementTypes.ATX_6 -> wrapChildren("h6")
                MarkdownElementTypes.BLOCK_QUOTE -> wrapChildren("blockquote")
                MarkdownElementTypes.PARAGRAPH -> {
                    sb.trimEnd()
                    wrapChildren("p", newline = true)
                }
                MarkdownElementTypes.CODE_SPAN -> {
                    val startDelimiter = node.child(MarkdownTokenTypes.BACKTICK)?.text
                    if (startDelimiter != null) {
                        val text = node.text.substring(startDelimiter.length).removeSuffix(startDelimiter)
                        sb.append("<code>").append(text.htmlEscape()).append("</code>")
                    }
                }
                MarkdownElementTypes.CODE_BLOCK,
                MarkdownElementTypes.CODE_FENCE -> {
                    sb.trimEnd()
                    sb.append("<pre><code>")
                    processChildren()
                    sb.append("</code></pre>")
                }
                MarkdownElementTypes.SHORT_REFERENCE_LINK,
                MarkdownElementTypes.FULL_REFERENCE_LINK -> {
                    val linkLabelNode = node.child(MarkdownElementTypes.LINK_LABEL)
                    val linkLabelContent = linkLabelNode?.children
                        ?.dropWhile { it.type == MarkdownTokenTypes.LBRACKET }
                        ?.dropLastWhile { it.type == MarkdownTokenTypes.RBRACKET }
                    if (linkLabelContent != null) {
                        val label = linkLabelContent.joinToString(separator = "") { it.text }
                        val linkText = node.child(MarkdownElementTypes.LINK_TEXT)?.toHtml() ?: label
                        DocumentationManagerUtil.createHyperlink(sb, label, linkText, true)
                    } else {
                        sb.append(node.text)
                    }
                }
                MarkdownElementTypes.INLINE_LINK -> {
                    val label = node.child(MarkdownElementTypes.LINK_TEXT)?.toHtml()
                    val destination = node.child(MarkdownElementTypes.LINK_DESTINATION)?.text
                    if (label != null && destination != null) {
                        sb.append("<a href=\"$destination\">$label</a>")
                    } else {
                        sb.append(node.text)
                    }
                }
                MarkdownTokenTypes.TEXT,
                MarkdownTokenTypes.WHITE_SPACE,
                MarkdownTokenTypes.COLON,
                MarkdownTokenTypes.SINGLE_QUOTE,
                MarkdownTokenTypes.DOUBLE_QUOTE,
                MarkdownTokenTypes.LPAREN,
                MarkdownTokenTypes.RPAREN,
                MarkdownTokenTypes.LBRACKET,
                MarkdownTokenTypes.RBRACKET,
                MarkdownTokenTypes.EXCLAMATION_MARK -> {
                    sb.append(nodeText)
                }
                MarkdownTokenTypes.CODE_LINE -> {
                    sb.append(nodeText.removePrefix(KDocTag.indentationWhiteSpaces).htmlEscape())
                }
                MarkdownTokenTypes.CODE_FENCE_CONTENT -> {
                    sb.append(nodeText.htmlEscape())
                }
                MarkdownTokenTypes.EOL -> {
                    val parentType = node.parent?.type
                    if (parentType == MarkdownElementTypes.CODE_BLOCK || parentType == MarkdownElementTypes.CODE_FENCE) {
                        sb.append("\n")
                    } else {
                        sb.append(" ")
                    }
                }
                MarkdownTokenTypes.GT -> sb.append("&gt;")
                MarkdownTokenTypes.LT -> sb.append("&lt;")

                MarkdownElementTypes.LINK_TEXT -> {
                    val childrenWithoutBrackets = node.children.drop(1).dropLast(1)
                    for (child in childrenWithoutBrackets) {
                        sb.append(child.toHtml())
                    }
                }

                MarkdownTokenTypes.EMPH -> {
                    val parentNodeType = node.parent?.type
                    if (parentNodeType != MarkdownElementTypes.EMPH && parentNodeType != MarkdownElementTypes.STRONG) {
                        sb.append(node.text)
                    }
                }

                else -> {
                    processChildren()
                }
            }
        }
        return sb.toString().trimEnd()
    }

    private fun StringBuilder.trimEnd() {
        while (length > 0 && this[length - 1] == ' ') {
            deleteCharAt(length - 1)
        }
    }

    private fun String.htmlEscape(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
