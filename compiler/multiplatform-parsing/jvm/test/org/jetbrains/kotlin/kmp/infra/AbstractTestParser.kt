/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

abstract class AbstractTestParser<T> {
    abstract fun parseKDocOnlyNodes(fileName: String, text: String): List<TestParseNode<out T>>

    abstract fun parse(fileName: String, text: String): TestParseNode<T>
}