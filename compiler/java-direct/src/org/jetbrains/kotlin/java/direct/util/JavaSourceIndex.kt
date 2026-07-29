/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct.util

import com.intellij.java.syntax.JavaSyntaxDefinition
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.java.syntax.parser.JavaKeywords
import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import com.intellij.platform.syntax.syntaxElementTypeSetOf
import com.intellij.pom.java.LanguageLevel
import java.io.File

private val TOP_LEVEL_TYPE_KEYWORDS = syntaxElementTypeSetOf(
    JavaSyntaxTokenType.CLASS_KEYWORD, JavaSyntaxTokenType.INTERFACE_KEYWORD, JavaSyntaxTokenType.ENUM_KEYWORD
)

internal data class LightweightFileInfo(
    val packageName: String?,
    val topLevelClassNames: Set<String>,
)

/**
 * Package and top-level type names via Java lexer, without a parse.
 * For PSI error-tolerant parity, tolerates a missing package `;` and unmatched closing braces/parentheses.
 * Modelled after `SingleJavaFileRootsIndex` TODO: merge implementations (KT-57845)
 */
internal fun extractFileInfoLightweight(file: File): LightweightFileInfo? {
    val fileContent = readJavaSourceFileText(file) ?: return null
    val lexer = JavaSyntaxDefinition.createLexer(LanguageLevel.HIGHEST).apply { start(fileContent) }

    var braceBalance = 0
    var parenthesisBalance = 0

    fun at(type: SyntaxElementType): Boolean = lexer.getTokenType() == type
    fun end(): Boolean = lexer.getTokenType() == null

    fun advance() {
        // Balances limited to be non-negative: to tolerate extra closing brackets while still being able to extract subsequent class names.
        when (lexer.getTokenType()) {
            JavaSyntaxTokenType.LBRACE -> braceBalance++
            JavaSyntaxTokenType.RBRACE -> if (braceBalance > 0) braceBalance--
            JavaSyntaxTokenType.LPARENTH -> parenthesisBalance++
            JavaSyntaxTokenType.RPARENTH -> if (parenthesisBalance > 0) parenthesisBalance--
        }
        lexer.advance()
    }

    // "record" is a soft keyword (IDENTIFIER); treat as a type only at top level.
    fun atRecord(): Boolean = at(JavaSyntaxTokenType.IDENTIFIER) && lexer.getTokenText() == JavaKeywords.RECORD

    fun atTypeDeclaration(): Boolean =
        braceBalance == 0 && parenthesisBalance == 0 && (lexer.getTokenType() in TOP_LEVEL_TYPE_KEYWORDS || atRecord())

    while (!end() && !at(JavaSyntaxTokenType.PACKAGE_KEYWORD) && !atTypeDeclaration()) {
        advance()
    }

    var packageName: String? = null
    if (at(JavaSyntaxTokenType.PACKAGE_KEYWORD)) {
        val name = StringBuilder()
        advance()
        loop@ while (!end() && !at(JavaSyntaxTokenType.SEMICOLON)) {
            val type = lexer.getTokenType()
            when {
                type == JavaSyntaxTokenType.IDENTIFIER || type == JavaSyntaxTokenType.DOT -> name.append(lexer.getTokenText())
                type == SyntaxTokenTypes.WHITE_SPACE || type in JavaSyntaxDefinition.comments -> Unit
                else -> break@loop
            }
            advance()
        }
        packageName = name.toString().takeIf { it.isNotEmpty() }
    }

    val classNames = mutableSetOf<String>()
    while (true) {
        while (!end() && !atTypeDeclaration()) advance()
        if (end()) break
        advance()
        while (!end() && !at(JavaSyntaxTokenType.IDENTIFIER)) advance()
        if (end()) break
        classNames.add(lexer.getTokenText())
    }

    if (classNames.isEmpty()) return null
    return LightweightFileInfo(packageName, classNames)
}
