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

package org.jetbrains.kotlin.idea.j2k

import com.intellij.ide.highlighter.HtmlFileType
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.XmlRecursiveElementVisitor
import com.intellij.psi.html.HtmlTag
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.javadoc.PsiInlineDocTag
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlText
import com.intellij.psi.xml.XmlTokenType
import org.jetbrains.kotlin.j2k.DocCommentConverter
import org.jetbrains.kotlin.j2k.content

object IdeaDocCommentConverter : DocCommentConverter {
    override fun convertDocComment(docComment: PsiDocComment): String {
        val html = buildString {
            appendJavadocElements(docComment.descriptionElements)

            tagsLoop@
            for (tag in docComment.tags) {
                when (tag.name) {
                    "deprecated" -> continue@tagsLoop
                    "see" -> append("@see ${convertJavadocLink(tag.content())}\n")
                    else -> appendJavadocElements(tag.children).append("\n")
                }
            }
        }

        if (html.trim().isEmpty() && docComment.findTagByName("deprecated") != null) {
            // @deprecated was the only content of the doc comment; we can drop the comment
            return ""
        }

        val htmlFile = PsiFileFactory.getInstance(docComment.project).createFileFromText(
                "javadoc.html", HtmlFileType.INSTANCE, html)
        val htmlToMarkdownConverter = HtmlToMarkdownConverter()
        htmlFile.accept(htmlToMarkdownConverter)
        return htmlToMarkdownConverter.result
    }

    private fun StringBuilder.appendJavadocElements(elements: Array<PsiElement>): StringBuilder {
        elements.forEach {
            if (it is PsiInlineDocTag) {
                append(convertInlineDocTag(it))
            }
            else {
                append(it.text)
            }
        }
        return this
    }

    private fun convertInlineDocTag(tag: PsiInlineDocTag) = when (tag.name) {
        "code", "literal" -> {
            val text = tag.dataElements.joinToString("") { it.text }
            val escaped = StringUtil.escapeXml(text.trimStart())
            if (tag.name == "code") "<code>$escaped</code>" else escaped
        }

        "link", "linkplain" -> {
            val valueElement = tag.linkElement()
            val labelText = tag.dataElements.firstOrNull { it is PsiDocToken }?.text ?: ""
            val kdocLink = convertJavadocLink(valueElement?.text)
            val linkText = if (labelText.isEmpty()) kdocLink else StringUtil.escapeXml(labelText)
            "<a docref=\"$kdocLink\">$linkText</a>"
        }

        else -> tag.text
    }

    private fun convertJavadocLink(link: String?): String =
            if (link != null) link.substringBefore('(').replace('#', '.') else ""

    private fun PsiDocTag.linkElement(): PsiElement? =
            valueElement ?: dataElements.firstOrNull { it !is PsiWhiteSpace }

    private fun XmlTag.attributesAsString() =
            if (attributes.isNotEmpty())
                attributes.joinToString(separator = " ", prefix = " ") { it.text }
            else
                ""

    private class HtmlToMarkdownConverter() : XmlRecursiveElementVisitor() {
        private enum class ListType { Ordered, Unordered; }
        data class MarkdownSpan(val prefix: String, val suffix: String) {
            companion object {
                val Empty = MarkdownSpan("", "")

                fun wrap(text: String) = MarkdownSpan(text, text)
                fun prefix(text: String) = MarkdownSpan(text, "")

                fun preserveTag(tag: XmlTag) =
                        MarkdownSpan("<${tag.name}${tag.attributesAsString()}>", "</${tag.name}>")
            }
        }


        val result: String
            get() = markdownBuilder.toString()

        private val markdownBuilder = StringBuilder("/**")
        private var afterLineBreak = false
        private var whitespaceIsPartOfText = true
        private var currentListType = ListType.Unordered

        override fun visitWhiteSpace(space: PsiWhiteSpace) {
            super.visitWhiteSpace(space)

            if (whitespaceIsPartOfText) {
                appendPendingText()
                markdownBuilder.append(space.text)
                if (space.textContains('\n')) {
                    afterLineBreak = true
                }
            }
        }

        override fun visitElement(element: PsiElement) {
            super.visitElement(element)

            val tokenType = element.node.elementType

            when (tokenType) {
                XmlTokenType.XML_DATA_CHARACTERS -> {
                    appendPendingText()
                    markdownBuilder.append(element.text)
                }
                XmlTokenType.XML_CHAR_ENTITY_REF -> {
                    appendPendingText()
                    val grandParent = element.parent.parent
                    if (grandParent is HtmlTag && (grandParent.name == "code" || grandParent.name == "literal"))
                        markdownBuilder.append(StringUtil.unescapeXml(element.text))
                    else
                        markdownBuilder.append(element.text)
                }
            }

        }

        override fun visitXmlTag(tag: XmlTag) {
            withWhitespaceAsPartOfText(false) {
                val oldListType = currentListType
                val atLineStart = afterLineBreak
                appendPendingText()
                val (openingMarkdown, closingMarkdown) = getMarkdownForTag(tag, atLineStart)
                markdownBuilder.append(openingMarkdown)

                super.visitXmlTag(tag)

                markdownBuilder.append(closingMarkdown)
                currentListType = oldListType
            }
        }

        override fun visitXmlText(text: XmlText) {
            withWhitespaceAsPartOfText(true) {
                super.visitXmlText(text)
            }
        }

        private inline fun withWhitespaceAsPartOfText(newValue: Boolean, block: () -> Unit) {
            val oldValue = whitespaceIsPartOfText
            whitespaceIsPartOfText = newValue
            try {
                block()
            }
            finally {
                whitespaceIsPartOfText = oldValue
            }
        }

        private fun getMarkdownForTag(tag: XmlTag, atLineStart: Boolean): MarkdownSpan = when (tag.name) {
            "b", "strong" -> MarkdownSpan.wrap("**")

            "p" -> if (atLineStart) MarkdownSpan.prefix("\n * ") else MarkdownSpan.prefix("\n *\n *")

            "i", "em" -> MarkdownSpan.wrap("*")

            "s", "del" -> MarkdownSpan.wrap("~~")

            "code" -> {
                val innerText = tag.value.text.trim()
                if (innerText.startsWith('`') && innerText.endsWith('`'))
                    MarkdownSpan("`` ", " ``")
                else
                    MarkdownSpan.wrap("`")
            }

            "a" -> {
                if (tag.getAttributeValue("docref") != null) {
                    val docRef = tag.getAttributeValue("docref")
                    val innerText = tag.value.text
                    if (docRef == innerText) MarkdownSpan("[", "]") else MarkdownSpan("[", "][$docRef]")
                }
                else if (tag.getAttributeValue("href") != null) {
                    MarkdownSpan("[", "](${tag.getAttributeValue("href") ?: ""})")
                }
                else {
                    MarkdownSpan.preserveTag(tag)
                }
            }

            "ul" -> {
                currentListType = ListType.Unordered; MarkdownSpan.Empty
            }

            "ol" -> {
                currentListType = ListType.Ordered; MarkdownSpan.Empty
            }

            "li" -> if (currentListType == ListType.Unordered) MarkdownSpan.prefix(" * ") else MarkdownSpan.prefix(" 1. ")

            else -> MarkdownSpan.preserveTag(tag)
        }

        private fun appendPendingText() {
            if (afterLineBreak) {
                markdownBuilder.append(" * ")
                afterLineBreak = false
            }
        }

        override fun visitXmlFile(file: XmlFile) {
            super.visitXmlFile(file)

            markdownBuilder.append(" */")
        }
    }
}
