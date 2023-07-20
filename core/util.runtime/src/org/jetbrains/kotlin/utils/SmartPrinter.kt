/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

class SmartPrinter(appendable: Appendable, indent: String = DEFAULT_INDENT) {
    companion object {
        private const val DEFAULT_INDENT = "    "
    }

    private val printer = Printer(appendable, indent)

    private var notFirstPrint: Boolean = false

    fun print(vararg objects: Any) {
        if (notFirstPrint) {
            printer.printWithNoIndent(*objects)
        } else {
            printer.print(*objects)
        }
        notFirstPrint = true
    }

    fun println(vararg objects: Any) {
        if (notFirstPrint) {
            printer.printlnWithNoIndent(*objects)
        } else {
            printer.println(*objects)
        }
        notFirstPrint = false
    }

    fun pushIndent() {
        printer.pushIndent()
    }

    fun popIndent() {
        printer.popIndent()
    }

    fun getCurrentIndentInUnits() = printer.currentIndentLengthInUnits
    fun getIndentUnit() = printer.indentUnitLength

    override fun toString(): String = printer.toString()
}

inline fun SmartPrinter.withIndent(block: () -> Unit) {
    pushIndent()
    block()
    popIndent()
}
