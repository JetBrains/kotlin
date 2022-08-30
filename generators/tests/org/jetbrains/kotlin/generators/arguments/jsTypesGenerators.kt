/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.arguments

import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer
import java.io.File

internal fun generateJsMainFunctionExecutionMode(
    apiDir: File,
    filePrinter: (targetFile: File, Printer.() -> Unit) -> Unit
) {
    val modeFqName = FqName("org.jetbrains.kotlin.gradle.dsl.JsMainFunctionExecutionMode")
    filePrinter(file(apiDir, modeFqName)) {
        generateDeclaration("enum class", modeFqName, afterType = "(val mode: String)") {
            val modes = hashMapOf(
                K2JsArgumentConstants::CALL.name to K2JsArgumentConstants.CALL,
                K2JsArgumentConstants::NO_CALL.name to K2JsArgumentConstants.NO_CALL
            )

            val lastIndex = modes.size - 1
            modes.entries.forEachIndexed { index, mode ->
                val lastChar = if (index == lastIndex) ";" else ","
                println("${mode.key}(\"${mode.value}\")$lastChar")
            }

            println()
            println("companion object {")
            withIndent {
                println("fun fromMode(mode: String): JsMainFunctionExecutionMode =")
                println("    JsMainFunctionExecutionMode.values().firstOrNull { it.mode == mode }")
                println("        ?: throw IllegalArgumentException(\"Unknown main function execution mode: ${'$'}mode\")")
            }
            println("}")
        }
    }
}
