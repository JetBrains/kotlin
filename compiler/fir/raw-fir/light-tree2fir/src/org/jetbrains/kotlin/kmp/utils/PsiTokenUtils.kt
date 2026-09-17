/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.utils

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtToken

/**
 * @receiver Some of Kotlin-based element types
 * @return Same integer ID as the corresponding [com.intellij.platform.syntax.SyntaxElementType] has.
 * If the element type is not Kotlin-based, the result is [org.jetbrains.kotlin.lexer.KtTokens.INVALID_Id].
 */
fun IElementType.kmpId(): Int =
    when {
        this is KtToken -> tokenId
        index < indexToId.size -> indexToId[index.toInt()]
        else -> org.jetbrains.kotlin.lexer.KtTokens.INVALID_Id
    }

private val indexToId: IntArray = run {
    val indexToIdMap = mutableMapOf<Short, Int>()

    var maxIndex: Short = 0
    var id = org.jetbrains.kotlin.kmp.parser.KtNodeTypes.FILE_ID
    KtNodeTypes::class.java.declaredFields.forEach {
        if (it.isAnnotationPresent(java.lang.Deprecated::class.java)) return@forEach
        val index = (it.get(null) as IElementType).index
        indexToIdMap[index] = id++
        if (index > maxIndex) {
            maxIndex = index
        }
    }
    org.jetbrains.kotlin.lexer.KtTokens.DOC_COMMENT.index.let {
        indexToIdMap[it] = KtTokens.DOC_COMMENT_ID
        if (it > maxIndex) {
            maxIndex = it
        }
    }
    org.jetbrains.kotlin.lexer.KtTokens.WHITE_SPACE.index.let {
        indexToIdMap[it] = KtTokens.WHITE_SPACE_ID
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
