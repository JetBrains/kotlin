/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.arguments

import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.utils.Printer
import java.io.File
import java.nio.file.Files

private fun printToString(fn: Printer.() -> Unit): String {
    val builder = StringBuilder()
    Printer(builder).fn()
    return builder.toString()
}

fun getPrinterForTests(file: File, fn: Printer.() -> Unit) {
    KtUsefulTestCase.assertSameLinesWithFile(file.absolutePath, printToString(fn))
}

fun getPrinterToFile(file: File, fn: Printer.() -> Unit) {
    Files.createDirectories(file.toPath().parent)
    file.writeText(printToString(fn))
}
