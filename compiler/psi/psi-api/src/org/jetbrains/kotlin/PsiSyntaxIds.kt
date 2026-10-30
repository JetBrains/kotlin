/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.psi.KtImplementationDetail

/**
 * @receiver A Kotlin element type
 * @return The ID from `syntax.common` that is consistent across all frontends (PSI/LightTree and KMP).
 * Only Kotlin tokens ([KtTokenId]), node types declared in [KtNodeTypeId], [org.jetbrains.kotlin.lexer.KtTokens.DOC_COMMENT]
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
    var id = KtNodeTypeId.FILE
    KtNodeTypes::class.java.declaredFields.forEach {
        if (it.isAnnotationPresent(java.lang.Deprecated::class.java)) return@forEach
        val index = (it.get(null) as IElementType).index
        indexToIdMap[index] = id++
        if (index > maxIndex) {
            maxIndex = index
        }
    }
    org.jetbrains.kotlin.lexer.KtTokens.DOC_COMMENT.index.let {
        indexToIdMap[it] = KtTokenId.DOC_COMMENT
        if (it > maxIndex) {
            maxIndex = it
        }
    }
    org.jetbrains.kotlin.lexer.KtTokens.WHITE_SPACE.index.let {
        indexToIdMap[it] = KtTokenId.WHITE_SPACE
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
