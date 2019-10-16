/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.AbstractCopyPasteTest
import org.jetbrains.kotlin.idea.test.dumpTextWithErrors
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils

abstract class AbstractInsertImportOnPasteTest : AbstractCopyPasteTest() {
    private val NO_ERRORS_DUMP_DIRECTIVE = "// NO_ERRORS_DUMP"
    private val DELETE_DEPENDENCIES_BEFORE_PASTE_DIRECTIVE = "// DELETE_DEPENDENCIES_BEFORE_PASTE"

    protected fun doTestCut(path: String) {
        doTestAction(IdeActions.ACTION_CUT, path)
    }

    protected fun doTestCopy(path: String) {
        doTestAction(IdeActions.ACTION_COPY, path)
    }

    private fun doTestAction(cutOrCopy: String, unused: String) {
        val testFile = testDataFile()
        val testFileText = FileUtil.loadFile(testFile, true)
        val testFileName = testFile.name

        val dependencyPsiFile1 = configureByDependencyIfExists(testFileName.replace(".kt", ".dependency.kt"))
        val dependencyPsiFile2 = configureByDependencyIfExists(testFileName.replace(".kt", ".dependency.java"))
        myFixture.configureByFile(testFileName)
        myFixture.performEditorAction(cutOrCopy)

        if (InTextDirectivesUtils.isDirectiveDefined(testFileText, DELETE_DEPENDENCIES_BEFORE_PASTE_DIRECTIVE)) {
            assert(dependencyPsiFile1 != null || dependencyPsiFile2 != null)
            runWriteAction {
                dependencyPsiFile1?.virtualFile?.delete(null)
                dependencyPsiFile2?.virtualFile?.delete(null)
            }
        }

        KotlinCopyPasteReferenceProcessor.declarationsToImportSuggested = emptyList()

        configureTargetFile(testFileName.replace(".kt", ".to.kt"))
        performNotWriteEditorAction(IdeActions.ACTION_PASTE)

        val namesToImportDump = KotlinCopyPasteReferenceProcessor.declarationsToImportSuggested.joinToString("\n")
        KotlinTestUtils.assertEqualsToFile(testDataFile(testFileName.replace(".kt", ".expected.names")), namesToImportDump)

        val resultFile = myFixture.file as KtFile
        val resultText = if (InTextDirectivesUtils.isDirectiveDefined(testFileText, NO_ERRORS_DUMP_DIRECTIVE))
            resultFile.text
        else
            resultFile.dumpTextWithErrors()
        KotlinTestUtils.assertEqualsToFile(testDataFile(testFileName.replace(".kt", ".expected.kt")), resultText)
    }
}
