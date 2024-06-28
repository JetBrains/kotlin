/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator

import org.jetbrains.kotlin.utils.SmartPrinter

private object ImportPrinter {
    fun SmartPrinter.printImports(imports: Collection<String>) {
        val importsToPrint = imports.filterNot { it.isDefaultImport() }.distinct().sorted()
        for (import in importsToPrint) {
            println("import $import")
        }
    }

    private fun String.isDefaultImport() = substringBeforeLast('.') in defaultImportedPackages

    private val defaultImportedPackages = setOf(
        "kotlin",
        "kotlin.annotation",
        "kotlin.collections",
        "kotlin.ranges",
        "kotlin.sequences",
        "kotlin.text",
        "kotlin.io",
    )
}

fun SmartPrinter.printImports(imports: Collection<String>) {
    with(ImportPrinter) { printImports(imports) }
}
