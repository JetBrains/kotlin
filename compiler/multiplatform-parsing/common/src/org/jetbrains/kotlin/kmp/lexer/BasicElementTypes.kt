/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.lexer

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import org.jetbrains.kotlin.syntax.SyntaxBasicElementTypeIds
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds

object BasicElementTypes : SyntaxElementTypesWithIds() {
    val WHITE_SPACE: SyntaxElementType = register(SyntaxBasicElementTypeIds.WHITE_SPACE, SyntaxTokenTypes.WHITE_SPACE)
    val BAD_CHARACTER: SyntaxElementType = register(SyntaxBasicElementTypeIds.BAD_CHARACTER, SyntaxTokenTypes.BAD_CHARACTER)
    val ERROR_ELEMENT: SyntaxElementType = register(SyntaxBasicElementTypeIds.ERROR_ELEMENT, SyntaxTokenTypes.ERROR_ELEMENT)
}
