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
import org.jetbrains.kotlin.psi.JetElementImpl
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

/**
 * A single part of a qualified name in the tag subject or link.
 */
public class KDocName(node: ASTNode): JetElementImpl(node) {
    public fun getContainingDoc(): KDoc {
        val kdoc = getStrictParentOfType<KDoc>()
        if (kdoc == null) {
            throw IllegalStateException("KDocName must be inside a KDoc")
        }
        return kdoc
    }

    public fun getQualifier(): KDocName? = getChildOfType()

    /**
     * Returns the range within the element containing the name (in other words,
     * the range of the element excluding the qualifier and dot, if present).
     */
    public fun getNameTextRange(): TextRange {
        val dot = getNode().findChildByType(JetTokens.DOT)
        val textRange = getTextRange()
        val nameStart = if (dot != null) dot.getTextRange().getEndOffset() - textRange.getStartOffset() else 0
        return TextRange(nameStart, textRange.getLength())
    }

    public fun getNameText(): String = getNameTextRange().substring(getText())

    public fun getQualifiedName(): List<String> {
        val qualifier = getQualifier()
        val nameAsList = listOf(getNameText())
        return if (qualifier != null) qualifier.getQualifiedName() + nameAsList else nameAsList
    }
}
