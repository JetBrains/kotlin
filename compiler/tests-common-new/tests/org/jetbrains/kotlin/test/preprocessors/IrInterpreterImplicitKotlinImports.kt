/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.preprocessors

import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestServices

class IrInterpreterImplicitKotlinImports(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    private fun String.addImplicitKotlinImport(fileName: String): String {
        val additionalImports = when (fileName) {
            "UByte.kt", "UShort.kt", "UInt.kt", "ULong.kt" -> listOf("kotlin.ranges.*")
            else -> listOf("kotlin.*", "kotlin.ranges.*", "kotlin.sequences.*", "kotlin.collections.*")
        }.joinToString(separator = "\n") { "import $it" }

        val lines = this.split("\n").toMutableList()
        when (val index = lines.indexOfFirst { it.startsWith("package ") }) {
            -1 -> lines.add(0, additionalImports)
            else -> lines.add(index + 1, additionalImports)
        }
        return lines.joinToString(separator = "\n")
    }

    override fun process(file: TestFile, content: String): String {
        if (!file.isAdditional) return content
        return content.addImplicitKotlinImport(file.name)
    }
}