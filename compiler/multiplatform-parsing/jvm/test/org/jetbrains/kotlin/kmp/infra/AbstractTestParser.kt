/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

abstract class AbstractTestParser<T> {
    abstract fun parse(fileName: String, text: String, kDocOnly: Boolean = false): TestParseNode<out T>

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