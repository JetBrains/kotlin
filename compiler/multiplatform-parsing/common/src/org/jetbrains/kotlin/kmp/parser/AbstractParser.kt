/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import com.intellij.platform.syntax.parser.WhitespaceOrCommentBindingPolicy

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
abstract class AbstractParser {
    abstract fun parse(builder: SyntaxTreeBuilder)

    abstract val whitespaces: Set<SyntaxElementType>

    abstract val comments: Set<SyntaxElementType>

    open val whitespaceOrCommentBindingPolicy: WhitespaceOrCommentBindingPolicy = object : WhitespaceOrCommentBindingPolicy {
        override fun isLeftBound(elementType: SyntaxElementType): Boolean {
            // `ERROR_ELEMENT` is treated is left bound by default in the old syntax lib.
            return elementType == SyntaxTokenTypes.ERROR_ELEMENT
        }
    }
}