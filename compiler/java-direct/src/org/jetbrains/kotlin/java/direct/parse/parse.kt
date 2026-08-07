/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct.parse

import com.intellij.java.syntax.JavaSyntaxDefinition
import com.intellij.java.syntax.parser.JavaParser
import com.intellij.platform.syntax.lexer.performLexing
import com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import com.intellij.platform.syntax.parser.SyntaxTreeBuilderFactory
import com.intellij.pom.java.LanguageLevel

fun parseJavaToSyntaxTreeBuilder(
    charSequence: CharSequence,
    start: Int,
): SyntaxTreeBuilder {
    val lexer = JavaSyntaxDefinition.createLexer(LanguageLevel.HIGHEST)

    val syntaxTreeBuilder = SyntaxTreeBuilderFactory.builder(
        charSequence,
        performLexing(charSequence, lexer, cancellationProvider = null, logger = null),
        whitespaces = JavaSyntaxDefinition.whitespaces,
        comments = JavaSyntaxDefinition.comments,
    ).withStartOffset(start)
        .withWhitespaceOrCommentBindingPolicy(JavaSyntaxDefinition.whitespaceOrCommentBindingPolicy)
        .build()

    // `Marker.rollbackTo` restores the lexeme index without re-arming the builder's
    // whitespace skip, so a rollback landing on leading trivia makes `tokenType` report the
    // whitespace/comment token itself. `FileParser` rolls back exactly that way when a file has
    // no package statement, which then hides `module` from `import module M;` recognition.
    // The builder always skips trivia in that path once a remapper is installed.
    syntaxTreeBuilder.setTokenTypeRemapper { source, _, _, _ -> source }

    parse(LanguageLevel.HIGHEST, syntaxTreeBuilder)
    return syntaxTreeBuilder
}

fun parse(languageLevel: LanguageLevel, builder: SyntaxTreeBuilder) {
    val parser = JavaParser(languageLevel)
    parser.fileParser.parse(builder)
}

/**
 * Convenience wrapper around [parseJavaToSyntaxTreeBuilder] + [buildJavaLightTree] for callers
 * that just need the resulting [JavaLightTree].
 */
fun parseJavaToLightTree(charSequence: CharSequence, start: Int): JavaLightTree {
    val builder = parseJavaToSyntaxTreeBuilder(charSequence, start)
    return buildJavaLightTree(builder, charSequence)
}

