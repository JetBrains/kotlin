/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser

import fleet.com.intellij.platform.syntax.SyntaxElementType
import fleet.com.intellij.platform.syntax.util.parser.SyntaxTreeBuilderAdapter

open class SemanticWhitespaceAwareSyntaxBuilderAdapter(private val delegateBuilder: SemanticWhitespaceAwareSyntaxBuilder) :
    SyntaxTreeBuilderAdapter(delegateBuilder), SemanticWhitespaceAwareSyntaxBuilder {
    override fun newlineBeforeCurrentToken(): Boolean {
        return delegateBuilder.newlineBeforeCurrentToken()
    }

    override fun disableNewlines() {
        delegateBuilder.disableNewlines()
    }

    override fun enableNewlines() {
        delegateBuilder.enableNewlines()
    }

    override fun restoreNewlinesState() {
        delegateBuilder.restoreNewlinesState()
    }

    override fun restoreJoiningComplexTokensState() {
        delegateBuilder.restoreJoiningComplexTokensState()
    }

    override fun enableJoiningComplexTokens() {
        delegateBuilder.enableJoiningComplexTokens()
    }

    override fun disableJoiningComplexTokens() {
        delegateBuilder.disableJoiningComplexTokens()
    }

    override fun isWhitespaceOrComment(elementType: SyntaxElementType): Boolean {
        return delegateBuilder.isWhitespaceOrComment(elementType)
    }
}
