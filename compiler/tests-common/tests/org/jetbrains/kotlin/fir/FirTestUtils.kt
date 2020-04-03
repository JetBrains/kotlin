/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.util.text.StringUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest.Companion.DIAGNOSTIC_IN_TESTDATA_PATTERN
import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest.Companion.SPEC_LINKS_IN_TESTDATA_PATTERN
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import java.io.File

fun compareAndMergeFirFileAndOldFrontendFile(
    oldFrontendTestDataFile: File,
    frontendIRTestDataFile: File,
    compareWithTrimming: Boolean = false
) {
    if (oldFrontendTestDataFile.exists() && frontendIRTestDataFile.exists()) {
        val originalLines = oldFrontendTestDataFile.readLines()
        val firLines = frontendIRTestDataFile.readLines()
        val sameDumps = if (compareWithTrimming) {
            firLines.withIndex().all { (index, line) ->
                val trimmed = line.trim()
                val originalTrimmed = originalLines.getOrNull(index)?.trim()
                trimmed.isEmpty() && originalTrimmed?.isEmpty() != false || trimmed == originalTrimmed
            } && originalLines.withIndex().all { (index, line) ->
                index < firLines.size || line.trim().isEmpty()
            }
        } else {
            firLines == originalLines
        }
        if (sameDumps) {
            frontendIRTestDataFile.delete()
            val oldFrontendTestDataFilePath = oldFrontendTestDataFile.absolutePath
            val fileWithFirIdentical = if (!oldFrontendTestDataFilePath.endsWith(".txt")) {
                oldFrontendTestDataFile
            } else {
                File(oldFrontendTestDataFilePath.replace(".txt", ".kt"))
            }
            fileWithFirIdentical.writeText("// FIR_IDENTICAL\n" + fileWithFirIdentical.readText())
        }
        TestCase.assertFalse(
            "Dumps via FIR & via old FE are the same. " +
                    "\nDeleted .fir.txt dump, added // FIR_IDENTICAL to test source" +
                    "\nPlease re-run the test now",
            sameDumps
        )
    }
}

fun loadTestDataWithoutDiagnostics(file: File): String {
    val textWithoutDiagnostics = KotlinTestUtils.doLoadFile(file)
        .replace(DIAGNOSTIC_IN_TESTDATA_PATTERN, "")
        .replace(SPEC_LINKS_IN_TESTDATA_PATTERN, "")
    return StringUtil.convertLineSeparators(textWithoutDiagnostics.trim()).trimTrailingWhitespacesAndAddNewlineAtEOF()
}
