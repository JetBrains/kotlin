/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser

import fleet.com.intellij.platform.syntax.SyntaxElementType
import fleet.com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import org.jetbrains.kotlin.kmp.lexer.KtTokens

abstract class AbstractKotlinParser() : AbstractParser() {
    override val whitespaces: Set<SyntaxElementType> = KtTokens.WHITESPACES
    override val comments: Set<SyntaxElementType> = KtTokens.COMMENTS

    protected fun createKotlinParsing(builder: SyntaxTreeBuilder): KotlinParsing {
        return KotlinParsing.createForTopLevel(SemanticWhitespaceAwareSyntaxBuilderImpl(builder))
    }
}

object KotlinParser : AbstractKotlinParser() {
    override fun parse(builder: SyntaxTreeBuilder) {
        createKotlinParsing(builder).parseFile()
    }
}

object KotlinScriptParser : AbstractKotlinParser() {
    override fun parse(builder: SyntaxTreeBuilder) {
        createKotlinParsing(builder).parseScript()
    }
}

object KotlinExpressionParser : AbstractKotlinParser() {
    override fun parse(builder: SyntaxTreeBuilder) {
        createKotlinParsing(builder).parseBlockExpression()
    }
}