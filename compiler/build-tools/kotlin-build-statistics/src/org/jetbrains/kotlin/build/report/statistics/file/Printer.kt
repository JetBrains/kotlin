/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.statistics.file

import java.io.IOException

private val LINE_SEPARATOR = System.getProperty("line.separator")

class Printer(
    private val out: Appendable,
    private val indentUnit: String = "  ",
    private var indent: String = ""
) {
    private fun append(s: String) {
        try {
            out.append(s)
        } catch (e: IOException) { // Do nothing
        }
    }

    fun println(vararg strings: String) {
        this.print(*strings)
        printLineSeparator()
    }

    private fun printLineSeparator() {
        append(LINE_SEPARATOR)
    }

    fun print(vararg strings: String) {
        if (strings.isNotEmpty()) {
            this.printIndent()
        }
        this.printWithNoIndent(*strings)
    }

    private fun printIndent() {
        append(indent)
    }

    private fun printWithNoIndent(vararg strings: String) {
        for (s in strings) {
            append(s)
        }
    }

    fun pushIndent() {
        indent += indentUnit
    }

    fun popIndent() {
        check(indent.length >= indentUnit.length) { "No indentation to pop" }
        indent = indent.substring(indentUnit.length)
    }

    inline fun <T> withIndent(headLine: String? = null, fn: () -> T): T {
        if (headLine != null) {
            this.println(headLine)
        }
        pushIndent()

        return try {
            fn()
        } finally {
            popIndent()
        }
    }
}