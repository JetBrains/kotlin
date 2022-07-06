/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.utils.Printer

open class FirPrinter(builder: StringBuilder) {
    private val printer = Printer(builder)

    private var lineBeginning = true

    fun print(vararg objects: Any) {
        if (lineBeginning) {
            lineBeginning = false
            printer.print(*objects)
        } else {
            printer.printWithNoIndent(*objects)
        }
    }

    fun println(vararg objects: Any) {
        print(*objects)
        printer.printlnWithNoIndent()
        lineBeginning = true
    }

    protected fun pushIndent() {
        printer.pushIndent()
    }

    protected fun popIndent() {
        printer.popIndent()
    }

    fun newLine() {
        println()
    }

    fun renderInBraces(leftBrace: String = "{", rightBrace: String = "}", f: () -> Unit) {
        println(" $leftBrace")
        pushIndent()
        f()
        popIndent()
        println(rightBrace)
    }

    override fun toString(): String {
        return printer.toString()
    }
}