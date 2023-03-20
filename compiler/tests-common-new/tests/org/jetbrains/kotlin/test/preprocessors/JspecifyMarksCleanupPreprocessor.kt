/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.preprocessors

import org.jetbrains.kotlin.test.frontend.classic.handlers.diagnosticsToJspecifyMarks
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.*

class JspecifyMarksCleanupPreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    override fun process(file: TestFile, content: String) = content.replace(regexToCleanup, "")

    companion object {
        private val jspecifyMarks = diagnosticsToJspecifyMarks.values.map { it.values }.flatten().joinToString("|")
        private val regexToCleanup = Regex("""[ ]*// ($jspecifyMarks)(, ($jspecifyMarks))*(?:\r\n|\n)""")
    }
}
