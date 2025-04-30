/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import org.jetbrains.kotlin.KtSourceFileLinesMapping

abstract class AbstractLexer<T> {
    abstract fun tokenize(text: String): List<Token<T>>
}

fun <T> List<Token<T>>.dump(sourceLinesMapping: KtSourceFileLinesMapping? = null): String =
    buildString { this@dump.forEach { appendDump(it, indent = 0, sourceLinesMapping) } }

sealed class Token<T>(val name: String, val start: Int, val end: Int, val token: T) {
    override fun toString(): String = StringBuilder().apply { appendDump(this@Token, indent = 0) }.toString()
}

class SingleToken<T>(name: String, start: Int, end: Int, token: T) : Token<T>(name, start, end, token)

class MultiToken<T>(name: String, start: Int, end: Int, token: T, val children: List<Token<T>>) : Token<T>(name, start, end, token)

private fun <T> StringBuilder.appendDump(token: Token<T>, indent: Int, sourceLinesMapping: KtSourceFileLinesMapping? = null) {
    if (isNotEmpty()) {
        appendLine()
    }
    (0 until indent).forEach { _ -> append("    ") }
    append(token.name)
    append(" [")

    fun appendLocation(location: Int) {
        if (sourceLinesMapping != null) {
            val (line, column) = sourceLinesMapping.getLineAndColumnByOffset(location)
            // It's more text-editor-friendly to start lines and columns with `1`
            append(line + 1)
            append(':')
            append(column + 1)
        } else {
            append(location)
        }
    }

    appendLocation(token.start)
    append("..")
    appendLocation(token.end)
    append(')')

    if (token is MultiToken<*>) {
        token.children.forEach { appendDump(it, indent + 1, sourceLinesMapping) }
    }
}