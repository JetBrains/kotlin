/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator

import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.builder.SYNTAX_DIAGNOSTIC_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.JS_DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.JVM_DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.NATIVE_DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.RegularDiagnosticData
import org.jetbrains.kotlin.fir.tree.generator.printer.printCopyright
import org.jetbrains.kotlin.fir.tree.generator.printer.printGeneratedMessage
import org.jetbrains.kotlin.fir.tree.generator.util.writeToFileUsingSmartPrinterIfFileContentChanged
import java.io.File

fun generateNonSuppressibleErrorNamesFile(generationPath: File, packageName: String) {
    getGenerationPath(generationPath, packageName).resolve("FirNonSuppressibleErrorNames.kt")
        .writeToFileUsingSmartPrinterIfFileContentChanged {
            printCopyright()
            println("package $packageName")
            println()
            printGeneratedMessage()
            println("val FIR_NON_SUPPRESSIBLE_ERROR_NAMES: Set<String> = setOf(")

            val combinedDiagnostics =
                DIAGNOSTICS_LIST + JVM_DIAGNOSTICS_LIST + JS_DIAGNOSTICS_LIST + NATIVE_DIAGNOSTICS_LIST + SYNTAX_DIAGNOSTIC_LIST
            for (diagnostic in combinedDiagnostics.allDiagnostics) {
                if (diagnostic is RegularDiagnosticData && diagnostic.severity == Severity.ERROR && !diagnostic.isSuppressible) {
                    println("    \"${diagnostic.name}\",")
                }
            }

            println(")")
        }
}