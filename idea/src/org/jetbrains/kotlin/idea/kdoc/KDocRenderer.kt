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

import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection

fun renderKDoc(docComment: KDocTag): String {
    val content = docComment.getContent()
    val result = StringBuilder("<p>")
    result.append(markdownToHtml(content))
    if (docComment is KDocSection) {
        val paramTags = docComment.findTagsByName("param").filter { it.getSubjectName() != null }
        renderTagList(paramTags, "Parameters", result)

        renderTag(docComment.findTagByName("return"), "Returns", result)

        val throwsTags = (docComment.findTagsByName("exception").union(docComment.findTagsByName("throws")))
                .filter { it.getSubjectName() != null }
        renderTagList(throwsTags, "Throws", result)

        renderTag(docComment.findTagByName("author"), "Author", result)
        renderTag(docComment.findTagByName("since"), "Since", result)
    }
    result.append("</p>")
    return result.toString()
}

private fun renderTagList(tags: List<KDocTag>, title: String, to: StringBuilder) {
    if (tags.isEmpty()) {
        return
    }
    to.append("<dl><dt><b>${title}:</b></dt>")
    tags.forEach {
        to.append("<dd><code>${it.getSubjectName()}</code> - ${markdownToHtml(it.getContent().trimLeading())}</dd>")
    }
    to.append("</dl>")
}

private fun renderTag(tag: KDocTag?, title: String, to: StringBuilder) {
    if (tag != null) {
        to.append("<dl><dt><b>${title}:</b></dt>")
        to.append("<dd>${markdownToHtml(tag.getContent())}</dd>")
        to.append("</dl>")
    }
}

fun markdownToHtml(markdown: String): String {
    // TODO Integrate a real Markdown parser
    return StringUtil.replace(markdown, "\n", "<br/>");
}
