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
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.parser.MarkdownParser
import org.intellij.markdown.parser.dialects.commonmark.CommonMarkMarkerProcessor
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag

object KDocRenderer {
    fun renderKDoc(docComment: KDocTag): String {
        val content = docComment.getContent()
        val result = StringBuilder("<p>")
        result.append(markdownToHtml(content))
        if (docComment is KDocSection) {
            result.append("\n")
            val paramTags = docComment.findTagsByName("param").filter { it.getSubjectName() != null }
            renderTagList(paramTags, "Parameters", result)

            renderTag(docComment.findTagByName("return"), "Returns", result)

            val throwsTags = (docComment.findTagsByName("exception").union(docComment.findTagsByName("throws")))
                    .filter { it.getSubjectName() != null }
            renderTagList(throwsTags, "Throws", result)

            renderTag(docComment.findTagByName("author"), "Author", result)
            renderTag(docComment.findTagByName("since"), "Since", result)

            renderSeeAlso(docComment, result)
        }
        result.append("</p>")
        return result.toString()
    }

    private fun renderSeeAlso(docComment: KDocSection, to: StringBuilder) {
        val seeTags = docComment.findTagsByName("see")
        if (seeTags.isEmpty()) return
        to.append("<DD><DL>")
        to.append("<DT><b>").append("See Also:").append("</b>")
        to.append("<DD>")
        seeTags.forEachIndexed { index, tag ->
            DocumentationManagerUtil.createHyperlink(to, tag.getSubjectName(), tag.getSubjectName(), false)
            if (index < seeTags.size() - 1) {
                to.append(", ")
            }
        }
        to.append("</DD></DL></DD>");
    }

    private fun renderTagList(tags: List<KDocTag>, title: String, to: StringBuilder) {
        if (tags.isEmpty()) {
            return
        }
        to.append("<dl><dt><b>${title}:</b></dt>")
        tags.forEach {
            to.append("<dd><code>${it.getSubjectName()}</code> - ${markdownToHtml(it.getContent().trimLeading())}</dd>")
        }
        to.append("</dl>\n")
    }

    private fun renderTag(tag: KDocTag?, title: String, to: StringBuilder) {
        if (tag != null) {
            to.append("<dl><dt><b>${title}:</b></dt>")
            to.append("<dd>${markdownToHtml(tag.getContent())}</dd>")
            to.append("</dl>\n")
        }
    }

    fun markdownToHtml(markdown: String): String {
        val markdownTree = MarkdownParser(CommonMarkMarkerProcessor.Factory).buildMarkdownTreeFromString(markdown)
        val markdownNode = MarkdownNode(markdownTree, null, markdown)

        // Avoid wrapping the entire converted contents in a <p> tag if it's just a single paragraph
        val maybeSingleParagraph = markdownNode.children.filter { it.type == MarkdownElementTypes.PARAGRAPH }.singleOrNull()
        if (maybeSingleParagraph != null) {
            return maybeSingleParagraph.children.map { it.toHtml() }.join("")
        } else {
            return markdownNode.toHtml()
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

    fun MarkdownNode.visit(action: (MarkdownNode, () -> Unit) -> Unit) {
        action(this) {
            for (child in children) {
                child.visit(action)
            }
        }
    }

    public fun MarkdownNode.toHtml(): String {
        val sb = StringBuilder()
        visit {(node, processChildren) ->
            val nodeType = node.type
            val nodeText = node.text
            when (nodeType) {
                MarkdownElementTypes.UNORDERED_LIST -> {
                    sb.appendln("<ul>")
                    processChildren()
                    sb.appendln("</ul>")
                }
                MarkdownElementTypes.ORDERED_LIST -> {
                    sb.appendln("<ol>")
                    processChildren()
                    sb.appendln("</ol>")
                }
                MarkdownElementTypes.LIST_ITEM -> {
                    sb.append("<li>")
                    processChildren()
                    sb.appendln("</li>")
                }
                MarkdownElementTypes.EMPH -> {
                    sb.append("<em>")
                    processChildren()
                    sb.append("</em>")
                }
                MarkdownElementTypes.STRONG -> {
                    sb.append("<strong>")
                    processChildren()
                    sb.append("</strong>")
                }
                MarkdownElementTypes.ATX_1 -> {
                    sb.append("<h1>")
                    processChildren()
                    sb.append("</h1>")
                }
                MarkdownElementTypes.ATX_2 -> {
                    sb.append("<h2>")
                    processChildren()
                    sb.append("</h2>")
                }
                MarkdownElementTypes.ATX_3 -> {
                    sb.append("<h3>")
                    processChildren()
                    sb.append("</h3>")
                }
                MarkdownElementTypes.ATX_4 -> {
                    sb.append("<h4>")
                    processChildren()
                    sb.append("</h4>")
                }
                MarkdownElementTypes.ATX_5 -> {
                    sb.append("<h5>")
                    processChildren()
                    sb.append("</h5>")
                }
                MarkdownElementTypes.ATX_6 -> {
                    sb.append("<h6>")
                    processChildren()
                    sb.append("</h6>")
                }
                MarkdownElementTypes.BLOCK_QUOTE -> {
                    sb.append("<blockquote>")
                    processChildren()
                    sb.append("</blockquote>")
                }
                MarkdownElementTypes.PARAGRAPH -> {
                    sb.append("<p>")
                    processChildren()
                    sb.appendln("</p>")
                }
                MarkdownElementTypes.CODE_SPAN -> {
                    sb.append("<code>")
                    processChildren()
                    sb.append("</code>")
                }
                MarkdownElementTypes.CODE_BLOCK -> {
                    sb.append("<pre><code>")
                    processChildren()
                    sb.append("</code><pre>")
                }
                MarkdownElementTypes.SHORT_REFERENCE_LINK,
                MarkdownElementTypes.FULL_REFERENCE_LINK -> {
                    val label = node.child(MarkdownElementTypes.LINK_LABEL)?.child(MarkdownTokenTypes.TEXT)?.text
                    if (label != null) {
                        val linkText = node.child(MarkdownElementTypes.LINK_TEXT)?.child(MarkdownTokenTypes.TEXT)?.text ?: label
                        DocumentationManagerUtil.createHyperlink(sb, label, linkText, true)
                    } else {
                        sb.append(node.text)
                    }
                }
                MarkdownElementTypes.INLINE_LINK -> {
                    val label = node.child(MarkdownElementTypes.LINK_TEXT)?.child(MarkdownTokenTypes.TEXT)?.text
                    val destination = node.child(MarkdownElementTypes.LINK_DESTINATION)?.text
                    if (label != null && destination != null) {
                        sb.append("a href=\"${destination}\">${label.htmlEscape()}</a>")
                    } else {
                        sb.append(node.text)
                    }
                }
                MarkdownTokenTypes.TEXT,
                MarkdownTokenTypes.WHITE_SPACE,
                MarkdownTokenTypes.COLON,
                MarkdownTokenTypes.DOUBLE_QUOTE,
                MarkdownTokenTypes.LPAREN,
                MarkdownTokenTypes.RPAREN,
                MarkdownTokenTypes.LBRACKET,
                MarkdownTokenTypes.RBRACKET -> {
                    sb.append(nodeText)
                }
                MarkdownTokenTypes.GT -> sb.append("&gt;")
                MarkdownTokenTypes.LT -> sb.append("&lt;")
                else -> {
                    processChildren()
                }
            }
        }
        return sb.toString()
    }

    fun String.htmlEscape(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
