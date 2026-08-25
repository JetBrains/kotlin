/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct.parse

import com.intellij.java.syntax.JavaSyntaxDefinition
import com.intellij.platform.syntax.lexer.performLexing
import com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import com.intellij.platform.syntax.parser.SyntaxTreeBuilderFactory
import com.intellij.pom.java.LanguageLevel

fun parseJavaToSyntaxTreeBuilder(
    charSequence: CharSequence,
    start: Int,
): SyntaxTreeBuilder {
    // `JDK_X` enables experimental features too
    val lexer = JavaSyntaxDefinition.createLexer(LanguageLevel.JDK_X)

    val syntaxTreeBuilder = SyntaxTreeBuilderFactory.builder(
        charSequence,
        performLexing(charSequence, lexer, cancellationProvider = null, logger = null),
        whitespaces = JavaSyntaxDefinition.whitespaces,
        comments = JavaSyntaxDefinition.comments,
    ).withStartOffset(start)
        .withWhitespaceOrCommentBindingPolicy(JavaSyntaxDefinition.whitespaceOrCommentBindingPolicy)
        .build()

    parse(LanguageLevel.JDK_X, syntaxTreeBuilder)
    return syntaxTreeBuilder
}

/**
 * The `JAVA_FILE` marker that [JavaSyntaxDefinition.parse] wraps around the parse is not decoration:
 * calling `JavaParser.fileParser` directly, without it, breaks the parse in two ways.
 * - The first `mark()` of a parse is the only one allowed to start on leading trivia; it then belongs to the
 *   package statement, whose rollback parks the lexer on that trivia, so a file starting with any trivia (a
 *   header comment, but a blank first line is enough) and having no `package` loses its whole import list.
 * - The whitespace balancer skips the outermost production, so the empty import list keeps its parse-time
 *   position and the first declaration can no longer bind the doc comment preceding it.
 *
 * [buildJavaLightTree] unwraps this node into its own root.
 */
fun parse(languageLevel: LanguageLevel, builder: SyntaxTreeBuilder) {
    JavaSyntaxDefinition.parse(languageLevel, builder)
}

/**
 * Convenience wrapper around [parseJavaToSyntaxTreeBuilder] + [buildJavaLightTree] for callers
 * that just need the resulting [JavaLightTree].
 */
fun parseJavaToLightTree(charSequence: CharSequence, start: Int): JavaLightTree {
    val builder = parseJavaToSyntaxTreeBuilder(charSequence, start)
    return buildJavaLightTree(builder, charSequence)
}

