/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.utils

import java.io.IOException

/**
 * A wrapper around an arbitrary [Appendable] making it convenient to print text with indentation.
 */
open class Printer private constructor(
    private val out: Appendable,
    private val maxBlankLines: Int,
    private val indentUnit: String,
    indent: String,
) : IndentingPrinter {
    final override var currentIndent = indent
        private set
    private var blankLineCountIncludingCurrent = 0
    private var withholdIndentOnce = false
    private var length = 0

    constructor(out: Appendable, indentUnit: String) : this(out, Int.MAX_VALUE, indentUnit)

    @JvmOverloads
    constructor(out: Appendable, maxBlankLines: Int = Int.MAX_VALUE, indentUnit: String = DEFAULT_INDENTATION_UNIT) : this(
        out,
        maxBlankLines,
        indentUnit,
        indent = "",
    )

    constructor(out: Appendable, parent: Printer) : this(out, parent.maxBlankLines, parent.indentUnit, parent.currentIndent)

    private fun append(o: Any?) {
        try {
            val string = o.toString()
            out.append(string)
            length += string.length
        } catch (e: IOException) {
            // Do nothing
        }
    }

    override fun println(vararg objects: Any?): Printer {
        print(*objects)
        printLineSeparator()
        return this
    }

    private fun printLineSeparator() {
        if (blankLineCountIncludingCurrent <= maxBlankLines) {
            blankLineCountIncludingCurrent++
            append(LINE_SEPARATOR)
        }
    }

    override fun print(vararg objects: Any?): Printer {
        if (withholdIndentOnce) {
            withholdIndentOnce = false
        } else if (objects.isNotEmpty()) {
            printIndent()
        }
        printWithNoIndent(*objects)
        return this
    }

    fun printIndent() {
        append(currentIndent)
    }

    fun printWithNoIndent(vararg objects: Any?): Printer {
        for (`object` in objects) {
            blankLineCountIncludingCurrent = 0
            append(`object`)
        }
        return this
    }

    fun withholdIndentOnce(): Printer {
        withholdIndentOnce = true
        return this
    }

    fun printlnWithNoIndent(vararg objects: Any?): Printer {
        printWithNoIndent(*objects)
        printLineSeparator()
        return this
    }

    override fun printlnMultiLine(s: String): IndentingPrinter {
        printlnWithNoIndent(
            s.replaceIndent(currentIndent)
                .lines()
                .joinToString(separator = "\n") { it.ifBlank { "" } }
        )
        return this
    }

    override fun pushIndent(): Printer {
        currentIndent += indentUnit
        return this
    }

    override fun popIndent(): Printer {
        check(currentIndent.length >= indentUnit.length) { "No indentation to pop" }
        currentIndent = currentIndent.substring(indentUnit.length)
        return this
    }

    fun separated(separator: Any, vararg items: Any?): Printer {
        for (i in items.indices) {
            if (i > 0) {
                printlnWithNoIndent(separator)
            }
            printlnWithNoIndent(items[i])
        }
        return this
    }

    fun separated(separator: Any, items: Collection<*>): Printer {
        val iterator = items.iterator()
        while (iterator.hasNext()) {
            printlnWithNoIndent(iterator.next())
            if (iterator.hasNext()) {
                printlnWithNoIndent(separator)
            }
        }
        return this
    }

    val isEmpty: Boolean
        get() = length == 0

    override fun toString(): String {
        return out.toString()
    }

    override val currentIndentLengthInUnits: Int
        get() = currentIndent.length / indentUnit.length

    override val indentUnitLength: Int
        get() = indentUnit.length

    companion object {
        private const val DEFAULT_INDENTATION_UNIT = "    "
        const val TWO_SPACE_INDENT = "  "

        @JvmField
        val LINE_SEPARATOR: String = System.getProperty("line.separator")
    }
}
