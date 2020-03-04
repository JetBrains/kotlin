/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import junit.framework.TestCase
import java.io.File

fun compareAndMergeFirFileAndOldFrontendFile(oldFrontendTestDataFile: File, frontendIRTestDataFile: File) {
    if (oldFrontendTestDataFile.exists() && frontendIRTestDataFile.exists()) {
        val originalLines = oldFrontendTestDataFile.readLines()
        val firLines = frontendIRTestDataFile.readLines()
        val sameDumps = firLines.withIndex().all { (index, line) ->
            val trimmed = line.trim()
            val originalTrimmed = originalLines.getOrNull(index)?.trim()
            trimmed.isEmpty() && originalTrimmed?.isEmpty() != false || trimmed == originalTrimmed
        } && originalLines.withIndex().all { (index, line) ->
            index < firLines.size || line.trim().isEmpty()
        }
        if (sameDumps) {
            frontendIRTestDataFile.delete()
            oldFrontendTestDataFile.writeText("// FIR_IDENTICAL\n" + oldFrontendTestDataFile.readText())
        }
        TestCase.assertFalse(
            "Dumps via FIR & via old FE are the same. Deleted .fir.txt dump, added // FIR_IDENTICAL to test source",
            sameDumps
        )
    }
}