/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDocumentManager
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.AbstractCopyPasteTest
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.refactoring.cutPaste.MoveDeclarationsEditorCookie
import org.jetbrains.kotlin.idea.refactoring.cutPaste.MoveDeclarationsProcessor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.dumpTextWithErrors
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractMoveOnCutPasteTest : AbstractCopyPasteTest() {
    private val BASE_PATH = PluginTestCaseBase.getTestDataPathBase() + "/copyPaste/moveDeclarations"

    private val OPTIMIZE_IMPORTS_AFTER_CUT_DIRECTIVE = "// OPTIMIZE_IMPORTS_AFTER_CUT"
    private val IS_AVAILABLE_DIRECTIVE = "// IS_AVAILABLE:"
    private val COPY_DIRECTIVE = "// COPY"

    override fun getTestDataPath() = BASE_PATH

    protected fun doTest(sourceFilePath: String) {
        myFixture.testDataPath = BASE_PATH
        val testFile = File(sourceFilePath)
        val sourceFileName = testFile.name
        val testFileText = FileUtil.loadFile(testFile, true)

        val dependencyFileName = sourceFileName.replace(".kt", ".dependency.kt")
        val dependencyPsiFile = configureByDependencyIfExists(dependencyFileName) as KtFile?
        val sourcePsiFile = myFixture.configureByFile(sourceFileName) as KtFile
        val useCopy = InTextDirectivesUtils.isDirectiveDefined(testFileText, COPY_DIRECTIVE)
        val caretMarker = myFixture.editor.document.createRangeMarker(myFixture.caretOffset, myFixture.caretOffset)
        myFixture.performEditorAction(if (useCopy) IdeActions.ACTION_COPY else IdeActions.ACTION_CUT)
        myFixture.editor.moveCaret(caretMarker.startOffset)
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        if (InTextDirectivesUtils.isDirectiveDefined(testFileText, OPTIMIZE_IMPORTS_AFTER_CUT_DIRECTIVE)) {
            OptimizeImportsProcessor(project, sourcePsiFile).run()
        }

        editor.putUserData(MoveDeclarationsEditorCookie.KEY, null) // because editor is reused

        val targetFileName = sourceFileName.replace(".kt", ".to.kt")
        val targetFileExists = File(testDataPath + File.separator + targetFileName).exists()
        val targetPsiFile = if (targetFileExists) configureTargetFile(targetFileName) else null
        performNotWriteEditorAction(IdeActions.ACTION_PASTE)

        val shouldBeAvailable = InTextDirectivesUtils.getPrefixedBoolean(testFileText, IS_AVAILABLE_DIRECTIVE) ?: true
        val cookie = editor.getUserData(MoveDeclarationsEditorCookie.KEY)
        val processor = cookie?.let { MoveDeclarationsProcessor.build(editor, cookie) }

        TestCase.assertEquals(shouldBeAvailable, processor != null)

        if (processor != null) {
            processor.performRefactoring()

            PsiDocumentManager.getInstance(project).commitAllDocuments()

            if (dependencyPsiFile != null) {
                KotlinTestUtils.assertEqualsToFile(File(BASE_PATH, dependencyFileName.replace(".kt", ".expected.kt")),
                                                   dependencyPsiFile.dumpTextWithErrors())
            }

            KotlinTestUtils.assertEqualsToFile(File(BASE_PATH, sourceFileName.replace(".kt", ".expected.kt")),
                                               sourcePsiFile.dumpTextWithErrors())
            if (targetPsiFile != null) {
                KotlinTestUtils.assertEqualsToFile(File(BASE_PATH, targetFileName.replace(".kt", ".expected.kt")),
                                                   targetPsiFile.dumpTextWithErrors())
            }
        }
    }
}
