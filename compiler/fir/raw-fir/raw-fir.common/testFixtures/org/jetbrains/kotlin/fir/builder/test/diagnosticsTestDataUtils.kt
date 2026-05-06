/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder.test

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import java.io.File

const val COMPILER_DIAGNOSTICS_TEST_DATA_DIRECTORY: String = "compiler/testData/diagnostics/tests"

/**
 * Returns a list of `(filePath, fileText)` pairs for all logical test data files in this [File]. The file texts are stripped of diagnostic
 * markup.
 *
 * The function is specifically designed to work with compiler diagnostic test data, covering special cases.
 */
fun File.toStrippedCompilerDiagnosticsTestDataFiles(): List<Pair<String, String>>? {
    when (this.path.replace("\\", "/")) {
        "compiler/testData/diagnostics/tests/constantEvaluator/constant/strings.kt" -> {
            // The regex in `stripDiagnosticMarkup` fails to correctly strip diagnostics from this file.
            return null
        }
    }

    val unstrippedText = FileUtil.loadFile(this, CharsetToolkit.UTF8, true).trim()
    val text = stripDiagnosticMarkup(unstrippedText).replaceAfter(".java", "")

    return splitTestDataIntoFiles(this.path, text)
}

private val DIAGNOSTIC_IN_TESTDATA_PATTERN = Regex("<!>|<!(.*?(\\(\".*?\"\\)|\\(\\))??)+(?<!<)!>")

/**
 * Strips diagnostic markup (e.g. `<!SOME_DIAGNOSTIC!>...<!>`) from test data text.
 */
private fun stripDiagnosticMarkup(text: String): String {
    return text.replace(DIAGNOSTIC_IN_TESTDATA_PATTERN, "")
}

/**
 * Splits multi-file test data by `// FILE:` directives into a list of (filePath, fileText) pairs.
 * Returns a single-element list with the original path and text if no directive is found.
 */
fun splitTestDataIntoFiles(filePath: String, text: String): List<Pair<String, String>> {
    val fileDirective = "// FILE:"
    val idx = text.indexOf(fileDirective)
    if (idx > 0 && text[idx - 1] != '\n') {
        //try to avoid splitting of sources
        return emptyList()
    }
    if (idx >= 0) {
        val result = mutableListOf<Pair<String, String>>()
        val strings = text.drop(idx).drop(fileDirective.length).split(fileDirective)
        for (string in strings) {
            val newLineIdx = string.indexOf("\n")
            if (newLineIdx < 0) return emptyList()
            result.add(Pair(string.substring(0, newLineIdx).trim(), string.substring(newLineIdx)))
        }
        return result
    }
    return listOf(Pair(filePath, text))
}
