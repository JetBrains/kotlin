/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.arguments

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer
import java.io.File

internal fun generateJvmTarget(
    apiDir: File,
    filePrinter: (targetFile: File, Printer.() -> Unit) -> Unit
) {
    val jvmTargetFqName = FqName("org.jetbrains.kotlin.gradle.dsl.JvmTarget")
    filePrinter(file(apiDir, jvmTargetFqName)) {
        generateDeclaration("enum class", jvmTargetFqName, afterType = "(val target: String)") {
            val jvmTargetValues = JvmTarget.values()
            val deprecatedJvmTargetValues = JvmTarget.values().subtract(JvmTarget.supportedValues().toSet())

            val lastIndex = jvmTargetValues.size - 1
            jvmTargetValues.forEachIndexed { index, jvmTarget ->
                val lastChar = if (index == lastIndex) ";" else ","
                val prefix = if (jvmTarget in deprecatedJvmTargetValues) {
                    "@Deprecated(\"Will be removed soon\") "
                } else {
                    ""
                }
                println("$prefix${jvmTarget.name}(\"${jvmTarget.description}\")$lastChar")
            }

            println()
            println("companion object {")
            withIndent {
                println("fun fromTarget(target: String): JvmTarget =")
                println("    JvmTarget.values().firstOrNull { it.target == target }")
                println("        ?: throw IllegalArgumentException(\"Unknown Kotlin JVM target: ${'$'}target\")")
            }
            println("}")
        }
    }
}