/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

abstract class AbstractLexer<T> {
    abstract fun tokenize(text: String): List<Token<T>>
}

fun <T> List<Token<T>>.dump(): String = buildString { this@dump.forEach { appendLine(it.dump()) } }

sealed class Token<T>(val name: String, val start: Int, val end: Int, val token: T) {
    open fun dump(indent: Int = 0): String {
        return "    ".repeat(indent) + "$name [$start..$end)"
    }

    override fun toString(): String = dump()
}

class SingleToken<T>(name: String, start: Int, end: Int, token: T) : Token<T>(name, start, end, token)

class MultiToken<T>(name: String, start: Int, end: Int, token: T, val children: List<Token<T>>) : Token<T>(name, start, end, token) {
    override fun dump(indent: Int): String {
        return buildString {
            appendLine(super.dump(indent))
            for ((index, child) in children.withIndex()) {
                append(child.dump(indent + 1))
                if (index < children.lastIndex) {
                    appendLine()
                }
            }
        }
    }
}