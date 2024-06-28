/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.arguments

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer
import java.io.File

/**
 * ApiVersion and LanguageVersion are almost the same in the compiler api, so Gradle DSL options
 * exposes KotlinVersion that covers both of them.
 */
internal fun generateKotlinVersion(
    apiDir: File,
    filePrinter: (targetFile: File, Printer.() -> Unit) -> Unit
) {
    val kotlinVersionFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinVersion")
    filePrinter(fileFromFqName(apiDir, kotlinVersionFqName)) {
        generateDeclaration("enum class", kotlinVersionFqName, afterType = "(val version: String)") {
            for (languageVersion in LanguageVersion.values()) {
                val prefix = when {
                    languageVersion.isUnsupported -> "@Deprecated(\"Unsupported\", level = DeprecationLevel.ERROR) "
                    languageVersion.isDeprecated -> "@Deprecated(\"Will be removed soon\") "
                    else -> ""
                }

                println("${prefix}KOTLIN_${languageVersion.major}_${languageVersion.minor}(\"${languageVersion.versionString}\"),")
            }
            println(";")

            println()
            println("companion object {")
            withIndent {
                println("@JvmStatic")
                println("fun fromVersion(version: String): KotlinVersion =")
                println("    KotlinVersion.values().firstOrNull { it.version == version }")
                println("        ?: throw IllegalArgumentException(\"Unknown Kotlin version: ${'$'}version\")")
                println()
                println("@JvmStatic")
                println("val DEFAULT = KOTLIN_${LanguageVersion.LATEST_STABLE.major}_${LanguageVersion.LATEST_STABLE.minor}")
            }
            println("}")
        }
    }
}
