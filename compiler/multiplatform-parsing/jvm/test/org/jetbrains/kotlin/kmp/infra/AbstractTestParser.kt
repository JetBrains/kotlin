/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import com.intellij.openapi.util.io.FileUtilRt

enum class ParseMode {
    /**
     * Blocks and lambdas remain collapsed.
     * It is maybe useful for working in IDE and testing with the same mode in PSI
     */
    NoCollapsableAndKDoc,

    /**
     * Useful when compiler used just for compiling from CLI (KDoc doesn't affect result artifacts)
     */
    NoKDoc,

    /**
     * Useful for testing and comparison with PSI
     */
    Full;
}

abstract class AbstractTestParser<T>(val parseMode: ParseMode) {
    abstract fun parse(fileName: String, text: String): TestParseNode<out T>

    protected fun isScript(fileName: String): Boolean = FileUtilRt.getExtension(fileName) == "kts"

    protected fun List<TestParseNode<out T>>.wrapRootsIfNeeded(end: Int): TestParseNode<out T> {
        return if (size != 1) {
            TestParseNode(
                TestSyntaxElement.WRAPPER_SYNTAX_ELEMENT_NAME,
                0,
                end,
                parseNode = null,
                this
            )
        } else {
            single()
        }
    }
}