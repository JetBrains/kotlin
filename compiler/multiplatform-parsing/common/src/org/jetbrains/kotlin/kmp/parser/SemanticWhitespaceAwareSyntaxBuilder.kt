/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser

import fleet.com.intellij.platform.syntax.SyntaxElementType
import fleet.com.intellij.platform.syntax.parser.SyntaxTreeBuilder

interface SemanticWhitespaceAwareSyntaxBuilder : SyntaxTreeBuilder {
    fun newlineBeforeCurrentToken(): Boolean
    fun disableNewlines()
    fun enableNewlines()
    fun restoreNewlinesState()

    fun restoreJoiningComplexTokensState()
    fun enableJoiningComplexTokens()
    fun disableJoiningComplexTokens()

    override fun isWhitespaceOrComment(elementType: SyntaxElementType): Boolean
}
