/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.util

import org.jetbrains.kotlin.utils.IndentingPrinter
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KType

// -------------------------------- imports --------------------------------

fun SmartPrinter.printImports(
    packageName: String,
    importableTypes: Collection<KType>,
    simpleImports: Collection<String>,
    starImports: Collection<String>,
) {
    val imports = collectImports(packageName, importableTypes, simpleImports, starImports)
    if (imports.isEmpty()) return
    printImports(imports)
    println()
}

private fun collectImports(
    packageName: String,
    importableTypes: Collection<KType>,
    simpleImports: Collection<String>,
    starImports: Collection<String>,
): Collection<String> {
    return buildSet {
        for (starImport in starImports) {
            add("$starImport.*")
        }

        importableTypes.forEach { type ->
            type.collectClassNamesTo(this)
        }
        addAll(simpleImports)
    }.filterNot { importString ->
        importString.dropLastWhile { it != '.' } == "$packageName."
    }
}

private fun KType.collectClassNamesTo(set: MutableSet<String>) {
    (classifier as? KClass<*>)?.qualifiedName?.let(set::add)
    for (argument in arguments) {
        argument.type?.collectClassNamesTo(set)
    }
}

private fun SmartPrinter.printImports(imports: Collection<String>) {
    val importsToPrint = imports.filterNot { it.isDefaultImport() }.distinct().sorted()
    for (import in importsToPrint) {
        println("import $import")
    }
}

private fun String.isDefaultImport(): Boolean {
    return substringBeforeLast('.') in defaultImportedPackages
}

private val defaultImportedPackages = setOf(
    "kotlin",
    "kotlin.annotation",
    "kotlin.collections",
    "kotlin.ranges",
    "kotlin.sequences",
    "kotlin.text",
    "kotlin.io",
)

// -------------------------------- disclaimers --------------------------------

private val COPYRIGHT = File("license/COPYRIGHT_HEADER.txt").readText()

fun SmartPrinter.printCopyright() {
    println(COPYRIGHT)
    println()
}

fun SmartPrinter.printGeneratedMessage() {
    println(GeneratorsFileUtil.GENERATED_MESSAGE)
    println()
}

// -------------------------------- other utils --------------------------------

fun getGenerationPath(rootPath: File, packageName: String): File {
    return packageName
        .split(".")
        .fold(rootPath, File::resolve)
        .apply { mkdirs() }
}

inline fun IndentingPrinter.printBlock(header: String = "", body: () -> Unit) {
    println("$header {")
    withIndent(body)
    println("}")
}
