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
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.parser.utils.KotlinParsing
import org.jetbrains.kotlin.kmp.parser.utils.SemanticWhitespaceAwareSyntaxBuilderImpl

@ApiStatus.Experimental
class KotlinParser(val isScript: Boolean, val isLazy: Boolean) : AbstractParser() {
    override val whitespaces: Set<SyntaxElementType> = KtTokens.WHITESPACES
    override val comments: Set<SyntaxElementType> = KtTokens.COMMENTS

    override val whitespaceOrCommentBindingPolicy: WhitespaceOrCommentBindingPolicy = object : WhitespaceOrCommentBindingPolicy {
        override fun isLeftBound(elementType: SyntaxElementType): Boolean {
            return elementType == SyntaxTokenTypes.ERROR_ELEMENT ||
                    elementType == KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL ||
                    elementType == KtNodeTypes.CONSTRUCTOR_DELEGATION_REFERENCE
        }
    }

    override fun parse(builder: SyntaxTreeBuilder) {
        val whitespaceAwareBuilder = SemanticWhitespaceAwareSyntaxBuilderImpl(builder)
        val builder = if (isLazy) {
            KotlinParsing.createForTopLevel(whitespaceAwareBuilder)
        } else {
            KotlinParsing.createForTopLevelNonLazy(whitespaceAwareBuilder)
        }
        if (isScript) {
            builder.parseScript()
        } else {
            builder.parseFile()
        }
    }
}