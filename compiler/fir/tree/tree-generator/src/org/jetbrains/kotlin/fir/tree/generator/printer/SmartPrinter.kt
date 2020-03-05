/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import java.io.File
import java.lang.Appendable

class SmartPrinter(appendable: Appendable) {
    private val printer = org.jetbrains.kotlin.utils.Printer(appendable)

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
}

inline fun SmartPrinter.withIndent(block: () -> Unit) {
    pushIndent()
    block()
    popIndent()
}

inline fun File.useSmartPrinter(block: SmartPrinter.() -> Unit) {
    writer().use { SmartPrinter(it).block() }
}