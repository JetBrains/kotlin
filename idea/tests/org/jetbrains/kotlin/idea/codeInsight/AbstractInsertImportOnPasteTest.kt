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
import org.jetbrains.kotlin.checkers.DebugInfoUtil
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.AbstractCopyPasteTest
import org.jetbrains.kotlin.idea.PluginTestCaseBase
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetReferenceExpression
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.JetTestUtils
import java.io.File
import kotlin.test.fail

public abstract class AbstractInsertImportOnPasteTest : AbstractCopyPasteTest() {
    private val BASE_PATH = PluginTestCaseBase.getTestDataPathBase() + "/copyPaste/imports"
    private val DEFAULT_TO_FILE_TEXT = "package to\n\n<caret>"
    private val ALLOW_UNRESOLVED_DIRECTIVE = "// ALLOW_UNRESOLVED"

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

        val toFileName = testFileName.replace(".kt", ".to.kt")
        val toFile = configureToFile(toFileName)
        performNotWriteEditorAction(IdeActions.ACTION_PASTE)

        val namesToImportDump = KotlinCopyPasteReferenceProcessor.declarationsToImportSuggested.joinToString("\n")
        JetTestUtils.assertEqualsToFile(File(path.replace(".kt", ".expected.names")), namesToImportDump)

        JetTestUtils.assertEqualsToFile(File(path.replace(".kt", ".expected.kt")), myFixture.getEditor().getDocument().getText())

        if (!InTextDirectivesUtils.isDirectiveDefined(FileUtil.loadFile(testFile, true), ALLOW_UNRESOLVED_DIRECTIVE)) {
            checkNoUnresolvedReferences(toFile)
        }
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

    private fun checkNoUnresolvedReferences(file: JetFile) {
        val bindingContext = file.analyzeFully()
        for (diagnostic in bindingContext.getDiagnostics()) {
            if (Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS.contains(diagnostic.getFactory())) {
                val textRanges = diagnostic.getTextRanges()
                val diagnosticText = DefaultErrorMessages.render(diagnostic)
                if (diagnostic.getPsiFile() == file) {
                    fail(diagnostic.getFactory().getName() + ": " + diagnosticText + " " + DiagnosticUtils.atLocation(file, textRanges.get(0)))
                }
            }
        }
        DebugInfoUtil.markDebugAnnotations(file, bindingContext, object : DebugInfoUtil.DebugInfoReporter() {
            override fun preProcessReference(expression: JetReferenceExpression) {
                expression.analyze(BodyResolveMode.FULL)
            }

            override fun reportElementWithErrorType(expression: JetReferenceExpression) {
                //do nothing
            }

            override fun reportMissingUnresolved(expression: JetReferenceExpression) {
                // this may happen if incorrect psi transformations are done
                fail(expression.getText() + " is unresolved but not marked " + DiagnosticUtils.atLocation(file, expression.getTextRange()))
            }

            override fun reportUnresolvedWithTarget(expression: JetReferenceExpression, target: String) {
                //do nothing
            }
        })
    }
}
