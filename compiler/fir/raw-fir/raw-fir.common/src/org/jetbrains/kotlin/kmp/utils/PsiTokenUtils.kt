/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.utils

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens

/**
 * @receiver Some of Kotlin-based element types
 * @return Same integer ID as the corresponding [com.intellij.platform.syntax.SyntaxElementType] has.
 * If the element type is not Kotlin-based, the result is [KtTokens.INVALID_Id].
 */
fun IElementType.kmpId(): Int =
    if (this is KtToken) tokenId else IdStorage.map[this] ?: KtTokens.INVALID_Id

private object IdStorage {
    val map = hashMapOf<IElementType, Int>()

    init {
        var id = org.jetbrains.kotlin.kmp.parser.KtNodeTypes.FILE_ID
        KtNodeTypes::class.java.declaredFields.forEach {
            if (it.isAnnotationPresent(java.lang.Deprecated::class.java)) return@forEach
            map[it.get(null) as IElementType] = id++
        }
    }
}
