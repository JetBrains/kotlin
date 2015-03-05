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
import com.intellij.psi.TokenType
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import com.intellij.psi.PsiElement

public open class KDocTag(node: ASTNode) : KDocElementImpl(node) {

    /**
     * Returns the name of this tag, not including the leading @ character.
     *
     * @return tag name or null if this tag represents the default section of a doc comment
     * or the code has a syntax error.
     */
    override fun getName(): String? {
        val tagName: PsiElement? = findChildByType(KDocTokens.TAG_NAME)
        if (tagName != null) {
            return tagName.getText().substring(1)
        }
        return null
    }

    /**
     * Returns the name of the entity documented by this tag (for example, the name of the parameter
     * for the @param tag), or null if this tag does not document any specific entity.
     */
    public open fun getSubjectName(): String? = getSubjectLink()?.getLinkText()

    public fun getSubjectLink(): KDocLink? {
        val children = childrenAfterTagName()
        if (hasSubject(children)) {
            return children.firstOrNull()?.getPsi() as? KDocLink
        }
        return null
    }

    public val knownTag: KDocKnownTag?
        get() {
            val name = getName()
            return if (name != null) KDocKnownTag.findByTagName(name) else null
        }

    private fun hasSubject(contentChildren: List<ASTNode>): Boolean {
        if (knownTag?.isReferenceRequired() ?: false) {
            return contentChildren.firstOrNull()?.getElementType() == KDocTokens.MARKDOWN_LINK
        }
        return false
    }

    private fun childrenAfterTagName(): List<ASTNode> =
        getNode().getChildren(null)
                .dropWhile { it.getElementType() == KDocTokens.TAG_NAME }
                .dropWhile { it.getElementType() == TokenType.WHITE_SPACE }

    /**
     * Returns the content of this tag (all text following the tag name and the subject if present,
     * with leading asterisks removed).
     */
    public open fun getContent(): String {
        val builder = StringBuilder()

        var contentStarted = false
        var afterAsterisk = false

        var children = childrenAfterTagName()
        if (hasSubject(children)) {
            children = children.drop(1)
        }
        for (node in children) {
            val type = node.getElementType()
            if (KDocTokens.CONTENT_TOKENS.contains(type)) {
                contentStarted = true
                builder.append(if (afterAsterisk) StringUtil.trimLeading(node.getText()) else node.getText())
                afterAsterisk = false
            }
            if (type == KDocTokens.LEADING_ASTERISK) {
                afterAsterisk = true
            }
            if (type == TokenType.WHITE_SPACE && contentStarted) {
                builder.append(StringUtil.repeat("\n", StringUtil.countNewLines(node.getText())))
            }
            if (type == KDocElementTypes.KDOC_TAG) {
                break
            }
        }

        return builder.toString()
    }
}
