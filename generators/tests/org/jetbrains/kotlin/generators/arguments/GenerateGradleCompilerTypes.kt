/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.arguments

import org.jetbrains.kotlin.utils.Printer
import java.io.File

fun generateGradleCompilerTypes(withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit) {
    val destDir = File("libraries/tools/kotlin-gradle-compiler-types/src/generated/kotlin")

    // Common
    generateKotlinVersion(destDir, withPrinterToFile)

    // Jvm
    generateJvmTarget(destDir, withPrinterToFile)

    // Js
    generateJsMainFunctionExecutionMode(destDir, withPrinterToFile)
    generateJsModuleKind(destDir, withPrinterToFile)
    generateJsSourceMapEmbedMode(destDir, withPrinterToFile)
    generateJsSourceMapNamesPolicy(destDir, withPrinterToFile)
    generateJsDiagnosticMode(destDir, withPrinterToFile)
}

fun main() {
    generateGradleCompilerTypes(::getPrinterToFile)
}
