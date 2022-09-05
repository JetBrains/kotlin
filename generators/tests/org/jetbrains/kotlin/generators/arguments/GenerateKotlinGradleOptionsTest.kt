/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.generators.arguments.test

import junit.framework.TestCase
import org.jetbrains.kotlin.generators.arguments.generateGradleCompilerTypes
import org.jetbrains.kotlin.generators.arguments.generateKotlinGradleOptions
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.utils.Printer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class GenerateKotlinGradleOptionsTest : TestCase() {
    fun testKotlinGradleOptionsAreUpToDate() {
        fun getPrinter(file: File, fn: Printer.() -> Unit) {
            val bytesOut = ByteArrayOutputStream()

            PrintStream(bytesOut).use {
                val printer = Printer(it)
                printer.fn()
            }

            val upToDateContent = bytesOut.toString()
            KtUsefulTestCase.assertSameLinesWithFile(file.absolutePath, upToDateContent)
        }

        generateKotlinGradleOptions(::getPrinter)
    }

    fun testKotlinGradleTypesAreUpToDate() {
        fun getPrinter(file: File, fn: Printer.() -> Unit) {
            val bytesOut = ByteArrayOutputStream()

            PrintStream(bytesOut).use {
                val printer = Printer(it)
                printer.fn()
            }

            val upToDateContent = bytesOut.toString()
            KtUsefulTestCase.assertSameLinesWithFile(file.absolutePath, upToDateContent)
        }

        generateGradleCompilerTypes(::getPrinter)
    }
}
