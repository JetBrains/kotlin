/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers.utils

import java.io.File

fun clearFileFromDiagnosticMarkup(file: File) {
    val text = file.readText()
    val cleanText = CheckerTestUtil.parseDiagnosedRanges(text, mutableListOf(), null) // will clear text from markup as side-effect
    file.writeText(cleanText)
}