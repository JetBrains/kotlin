/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.AbstractCopyPasteTest
import org.jetbrains.kotlin.idea.PluginTestCaseBase
import org.jetbrains.kotlin.idea.testUtils.dumpTextWithErrors
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.JetTestUtils
import java.io.File

public abstract class AbstractInsertImportOnPasteTest : AbstractCopyPasteTest() {
    private val BASE_PATH = PluginTestCaseBase.getTestDataPathBase() + "/copyPaste/imports"
    private val DEFAULT_TO_FILE_TEXT = "package to\n\n<caret>"
    private val NO_ERRORS_DUMP_DIRECTIVE = "// NO_ERRORS_DUMP"

    override fun getTestDataPath() = BASE_PATH

    protected fun doTestCut(path: String) {
        doTestAction(IdeActions.ACTION_CUT, path)
    }

    protected fun doTestCopy(path: String) {
        doTestAction(IdeActions.ACTION_COPY, path)
    }

    private fun doTestAction(cutOrCopy: String, path: String) {
        myFixture.setTestDataPath(BASE_PATH)
        val testFile = File(path)
        val testFileName = testFile.getName()

        configureByDependencyIfExists(testFileName.replace(".kt", ".dependency.kt"))
        configureByDependencyIfExists(testFileName.replace(".kt", ".dependency.java"))
        myFixture.configureByFile(testFileName)
        myFixture.performEditorAction(cutOrCopy)

        KotlinCopyPasteReferenceProcessor.declarationsToImportSuggested = emptyList()

        configureToFile(testFileName.replace(".kt", ".to.kt"))
        performNotWriteEditorAction(IdeActions.ACTION_PASTE)

        val namesToImportDump = KotlinCopyPasteReferenceProcessor.declarationsToImportSuggested.joinToString("\n")
        JetTestUtils.assertEqualsToFile(File(path.replace(".kt", ".expected.names")), namesToImportDump)

        val resultFile = myFixture.getFile() as JetFile
        val resultText = if (InTextDirectivesUtils.isDirectiveDefined(FileUtil.loadFile(testFile, true), NO_ERRORS_DUMP_DIRECTIVE))
            resultFile.getText()
        else
            resultFile.dumpTextWithErrors()
        JetTestUtils.assertEqualsToFile(File(path.replace(".kt", ".expected.kt")), resultText)
    }

    private fun configureToFile(toFileName: String): JetFile {
        if (File(BASE_PATH + "/" + toFileName).exists()) {
            return myFixture.configureByFile(toFileName) as JetFile
        }
        else {
            return myFixture.configureByText(toFileName, DEFAULT_TO_FILE_TEXT) as JetFile
        }
    }

    private fun configureByDependencyIfExists(dependencyFileName: String) {
        val file = File(BASE_PATH + "/" + dependencyFileName)
        if (file.exists()) {
            if (dependencyFileName.endsWith(".java")) {
                //allow test framework to put it under right directory
                myFixture.addClass(FileUtil.loadFile(file, true))
            }
            else {
                myFixture.configureByFile(dependencyFileName)
            }
        }
    }
}
