/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.syntax.NO_ID
import org.jetbrains.kotlin.syntax.SyntaxNodeTypeIds
import org.jetbrains.kotlin.syntax.SyntaxTokenIds

/**
 * @receiver A Kotlin element type
 * @return The ID from `syntax.common` that is consistent across all frontends (PSI/LightTree and KMP).
 * Only Kotlin tokens ([SyntaxTokenIds]), node types declared in [SyntaxNodeTypeIds], [org.jetbrains.kotlin.lexer.KtTokens.DOC_COMMENT]
 * and [org.jetbrains.kotlin.lexer.KtTokens.WHITE_SPACE] are mapped.
 * For other element types (e.g., KDoc element types or non-Kotlin ones), the result is [NO_ID].
 */
@KtImplementationDetail
fun IElementType.syntaxId(): Int =
    when {
        this is KtToken -> tokenId
        index < indexToId.size -> indexToId[index.toInt()]
        else -> NO_ID
    }

private val indexToId: IntArray = run {
    val indexToIdMap = mutableMapOf<Short, Int>()

    var maxIndex: Short = 0
    var id = SyntaxNodeTypeIds.FILE
    KtNodeTypes::class.java.declaredFields.forEach {
        if (it.isAnnotationPresent(java.lang.Deprecated::class.java)) return@forEach
        val index = (it.get(null) as IElementType).index
        indexToIdMap[index] = id++
        if (index > maxIndex) {
            maxIndex = index
        }
    }
    org.jetbrains.kotlin.lexer.KtTokens.DOC_COMMENT.index.let {
        indexToIdMap[it] = SyntaxTokenIds.DOC_COMMENT
        if (it > maxIndex) {
            maxIndex = it
        }
    }
    org.jetbrains.kotlin.lexer.KtTokens.WHITE_SPACE.index.let {
        indexToIdMap[it] = SyntaxTokenIds.WHITE_SPACE
        if (it > maxIndex) {
            maxIndex = it
        }
    }

    IntArray(maxIndex.toInt() + 1).apply {
        indexToIdMap.forEach { [index, id] ->
            this[index.toInt()] = id
        }
    }
}
