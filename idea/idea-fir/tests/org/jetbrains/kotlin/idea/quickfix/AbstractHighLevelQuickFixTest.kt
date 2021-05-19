/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.InspectionProfileEntry
import org.jetbrains.kotlin.test.utils.IgnoreTests
import java.io.File
import java.nio.file.Paths


abstract class AbstractHighLevelQuickFixTest : AbstractQuickFixTest() {
    override fun doTest(beforeFileName: String) {
        val firBeforeFileName = getFirBeforeFileName(beforeFileName)
        IgnoreTests.runTestIfNotDisabledByFileDirective(
            Paths.get(firBeforeFileName),
            disableTestDirective = IgnoreTests.DIRECTIVES.IGNORE_FIR_MULTILINE_COMMENT,
            directivePosition = IgnoreTests.DirectivePosition.LAST_LINE_IN_FILE,
            additionalFilesExtensions = arrayOf("after")
        ) {
            super.doTest(firBeforeFileName)
        }
    }

    private fun getFirBeforeFileName(beforeFileName: String): String {
        val firBeforeFilename = beforeFileName.replace(".kt", ".fir.kt")
        val firBeforeFile = File(firBeforeFilename)
        return if (firBeforeFile.exists()) {
            firBeforeFile.canonicalPath
        } else {
            beforeFileName
        }
    }

    override fun getAfterFileName(beforeFileName: String): String {
        val firAfterFile = File(testPath(beforeFileName.replace(".kt", ".fir.kt.after")))
        return if (firAfterFile.exists()) {
            firAfterFile.name
        } else {
            super.getAfterFileName(beforeFileName)
        }
    }

    // TODO: Enable these as more actions/inspections are enabled, and/or add more FIR-specific directives
    override fun checkForUnexpectedErrors() {}
    override fun checkAvailableActionsAreExpected(actions: List<IntentionAction>) {}
    override fun parseInspectionsToEnable(beforeFileName: String, beforeFileText: String): List<InspectionProfileEntry> {
        return emptyList()
    }
}
