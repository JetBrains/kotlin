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
import org.jetbrains.kotlin.kmp.tree.LightSyntaxTree
import org.jetbrains.kotlin.kmp.tree.buildLanguageSpecificLightTree

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

    parse(LanguageLevel.JDK_X, syntaxTreeBuilder)
    return syntaxTreeBuilder
}

fun parse(languageLevel: LanguageLevel, builder: SyntaxTreeBuilder) {
    JavaSyntaxDefinition.parse(languageLevel, builder)
}

/**
 * Convenience wrapper around [parseJavaToSyntaxTreeBuilder] + [buildLanguageSpecificLightTree] for callers
 * that just need the resulting [LightSyntaxTree].
 */
fun parseJavaToLightTree(charSequence: CharSequence, start: Int): LightSyntaxTree {
    val builder = parseJavaToSyntaxTreeBuilder(charSequence, start)
    return buildLanguageSpecificLightTree(
        builder, charSequence,
        buildLanguageSpecificTreeStructure = { JavaLightTreeStructure(it) },
        isComment = { it in JavaSyntaxDefinition.comments },
    )
}

