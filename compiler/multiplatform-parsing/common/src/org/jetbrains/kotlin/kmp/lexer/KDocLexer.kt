/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.lexer

import fleet.com.intellij.platform.syntax.asSyntaxElementTypeSet
import fleet.com.intellij.platform.syntax.util.lexer.FlexAdapter
import fleet.com.intellij.platform.syntax.util.lexer.MergingLexerAdapter

class KDocLexer : MergingLexerAdapter(
    FlexAdapter(KDocFlexLexer()),
    KDOC_TOKENS
) {
    companion object {
        val KDOC_TOKENS = listOf(KDocTokens.TEXT, KDocTokens.CODE_BLOCK_TEXT).asSyntaxElementTypeSet()
    }
}