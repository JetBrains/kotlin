/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.utils.Printer

abstract class FirPrinter internal constructor(builder: StringBuilder) {
    private val printer = Printer(builder)

    private var lineBeginning = true

    protected fun print(vararg objects: Any) {
        if (lineBeginning) {
            lineBeginning = false
            printer.print(*objects)
        } else {
            printer.printWithNoIndent(*objects)
        }
    }

    protected fun println(vararg objects: Any) {
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

    protected fun newLine() {
        println()
    }

}