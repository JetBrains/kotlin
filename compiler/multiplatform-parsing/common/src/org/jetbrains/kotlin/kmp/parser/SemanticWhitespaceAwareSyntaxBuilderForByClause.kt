/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser

class SemanticWhitespaceAwareSyntaxBuilderForByClause(builder: SemanticWhitespaceAwareSyntaxBuilder) :
    SemanticWhitespaceAwareSyntaxBuilderAdapter(builder) {
    var stackSize: Int = 0
        private set

    override fun disableNewlines() {
        super.disableNewlines()
        stackSize++
    }

    override fun enableNewlines() {
        super.enableNewlines()
        stackSize++
    }

    override fun restoreNewlinesState() {
        super.restoreNewlinesState()
        stackSize--
    }
}
