/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kdoc.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElementImpl
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolution.KtResolvable

/**
 * A single part of a qualified name in the tag subject or link.
 */
@OptIn(KtExperimentalApi::class)
class KDocName(node: ASTNode) : KtElementImpl(node), KtResolvable {
    fun getContainingDoc(): KDoc {
        val kdoc = getStrictParentOfType<KDoc>()
        return kdoc ?: throw IllegalStateException("KDocName must be inside a KDoc")
    }

    fun getContainingSection(): KDocSection {
        val kdoc = getStrictParentOfType<KDocSection>()
        return kdoc ?: throw IllegalStateException("KDocName must be inside a KDocSection")
    }

    fun getQualifier(): KDocName? = getChildOfType()

    /**
     * Returns the range within the element containing the name (in other words,
     * the range of the element excluding the qualifier and dot, if present).
     */
    fun getNameTextRange(): TextRange {
        val dot = node.findChildByType(KtTokens.DOT)
        val textRange = textRange
        var nameStart = if (dot != null) dot.textRange.endOffset - textRange.startOffset else 0
        var nameEnd = textRange.length
        if (nameEnd - nameStart >= 2 && text[nameStart] == '`' && text[nameEnd - 1] == '`') { // Unquote the identifier
            nameStart++
            nameEnd--
        }
        return TextRange(nameStart, nameEnd)
    }

    fun getNameText(): String = getNameTextRange().substring(text)

    fun getQualifiedName(): List<String> {
        val qualifier = getQualifier()
        val nameAsList = listOf(getNameText())
        return if (qualifier != null) qualifier.getQualifiedName() + nameAsList else nameAsList
    }

    fun getQualifiedNameAsFqName(): FqName {
        return FqName.fromSegments(getQualifiedName())
    }
}
