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

/** Lightweight Java-lexer scan for package name and top-level class names (no full parse). */

private val TOP_LEVEL_TYPE_KEYWORDS = syntaxElementTypeSetOf(
    JavaSyntaxTokenType.CLASS_KEYWORD, JavaSyntaxTokenType.INTERFACE_KEYWORD, JavaSyntaxTokenType.ENUM_KEYWORD
)

/**
 * Result of lightweight (no-parse) file scanning.
 */
internal data class LightweightFileInfo(
    val packageName: String?,
    val topLevelClassNames: Set<String>,
)

/**
 * Package and top-level type names via Java lexer (no full parse).
 * Tolerates a missing package `;` for PSI error-tolerant parity.
 */
internal fun extractFileInfoLightweight(file: File, reader: JavaSourceFileReader): LightweightFileInfo? {
    val fileContent = reader.readFileContent(file) ?: return null
    val lexer = JavaSyntaxDefinition.createLexer(LanguageLevel.HIGHEST).apply { start(fileContent) }

    var braceBalance = 0
    var parenthesisBalance = 0

    fun at(type: SyntaxElementType): Boolean = lexer.getTokenType() == type
    fun end(): Boolean = lexer.getTokenType() == null

    fun advance() {
        when {
            at(JavaSyntaxTokenType.LBRACE) -> braceBalance++
            at(JavaSyntaxTokenType.RBRACE) -> braceBalance--
            at(JavaSyntaxTokenType.LPARENTH) -> parenthesisBalance++
            at(JavaSyntaxTokenType.RPARENTH) -> parenthesisBalance--
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
                // Missing `;` is accepted (PSI error-tolerant); any other token ends the package decl.
                else -> break@loop
            }
            advance()
        }
        if (at(JavaSyntaxTokenType.SEMICOLON)) advance()
        packageName = name.toString()
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
