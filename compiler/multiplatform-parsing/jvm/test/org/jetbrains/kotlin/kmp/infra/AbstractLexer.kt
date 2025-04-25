/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

abstract class AbstractLexer<T> {
    abstract fun tokenize(text: String): List<TokenInfo<T>>
}

data class TokenInfo<T>(val name: String, val start: Int, val end: Int, val token: T) {
    fun render(): String {
        return "$name [$start..$end)"
    }
}

fun <T> List<TokenInfo<T>>.dump(): String = joinToString("\n") { it.render() }