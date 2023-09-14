/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree.printer

import org.jetbrains.kotlin.utils.SmartPrinter
import java.io.File

private val COPYRIGHT by lazy { File("license/COPYRIGHT_HEADER.txt").readText() }

class GeneratedFile(val file: File, val newText: String)

private fun getPathForFile(generationPath: File, packageName: String, typeName: String): File {
    val dir = generationPath.resolve(packageName.replace(".", "/"))
    return File(dir, "$typeName.kt")
}

fun printGeneratedType(
    generationPath: File,
    treeGeneratorReadMe: String,
    packageName: String,
    typeName: String,
    fileSuppressions: List<String> = emptyList(),
    body: SmartPrinter.() -> Unit,
): GeneratedFile {
    val stringBuilder = StringBuilder()
    val file = getPathForFile(generationPath, packageName, typeName)
    SmartPrinter(stringBuilder).body()
    return GeneratedFile(
        file,
        buildString {
            appendLine(COPYRIGHT)
            appendLine()
            append("// This file was generated automatically. See ")
            append(treeGeneratorReadMe)
            appendLine(".")
            appendLine("// DO NOT MODIFY IT MANUALLY.")
            appendLine()
            if (fileSuppressions.isNotEmpty()) {
                fileSuppressions.joinTo(this, prefix = "@file:Suppress(", postfix = ")\n\n") { "\"$it\"" }
            }
            appendLine("package $packageName")
            appendLine()
            append(stringBuilder)
        }
    )
}