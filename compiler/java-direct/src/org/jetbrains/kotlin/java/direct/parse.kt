/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.JavaSyntaxDefinition
import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.parser.JavaParser
import com.intellij.platform.syntax.lexer.performLexing
import com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import com.intellij.platform.syntax.parser.SyntaxTreeBuilderFactory
import fleet.com.intellij.multiplatform.pom.java.LanguageLevel

fun parseJavaToSyntaxTreeBuilder(
    charSequence: CharSequence,
    start: Int,
): SyntaxTreeBuilder {
    val lexer = JavaSyntaxDefinition.createLexer(LanguageLevel.HIGHEST)

    val syntaxTreeBuilder = SyntaxTreeBuilderFactory.builder(
        charSequence,
        performLexing(charSequence, lexer, cancellationProvider = null, logger = null),
        whitespaces = JavaSyntaxDefinition.whitespaces,
        comments = JavaSyntaxDefinition.commentSet,
    ).withStartOffset(start)
        .withWhitespaceOrCommentBindingPolicy(JavaSyntaxDefinition.getWhitespaceOrCommentBindingPolicy())
        .build()

    parse(LanguageLevel.HIGHEST, syntaxTreeBuilder)
    return syntaxTreeBuilder
}

fun parse(languageLevel: LanguageLevel, builder: SyntaxTreeBuilder) {
    val root = builder.mark()
    val parser = JavaParser(languageLevel)
    parser.fileParser.parse(builder)
    root.done(JavaSyntaxElementType.JAVA_FILE)
}

