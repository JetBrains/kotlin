/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.refactoring.move

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import org.jetbrains.jet.plugin.PluginTestCaseBase
import java.io.File
import com.intellij.refactoring.BaseRefactoringProcessor.ConflictsInTestsException
import com.google.gson.JsonObject
import com.intellij.refactoring.MultiFileTestCase
import com.intellij.openapi.vfs.VirtualFile
import com.google.gson.JsonParser
import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.Document
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.openapi.util.Computable
import org.jetbrains.jet.JetTestUtils
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

public abstract class AbstractJetMoveTest : MultiFileTestCase() {
    protected fun doTest(path: String) {
        fun extractCaretOffset(doc: Document): Int {
            return ApplicationManager.getApplication()!!.runWriteAction(
                    Computable<Int> {
                        val text = StringBuilder(doc.getText())
                        val offset = text.indexOf("<caret>")

                        if (offset >= 0) {
                            text.delete(offset, offset + "<caret>".length)
                            doc.setText(text.toString())
                        }

                        offset
                    }
            )!!
        }

        val config = JsonParser().parse(FileUtil.loadFile(File(path))) as JsonObject

        val action = MoveAction.valueOf(config.getString("type"))

        val testDir = path.substring(0, path.lastIndexOf("/"))
        val mainFilePath = config.getNullableString("mainFile")!!

        val conflictFile = File(testDir + "/conflicts.txt")
        doTest { rootDir, rootAfter ->
            val mainFile = rootDir.findFileByRelativePath(mainFilePath)!!
            val mainPsiFile = PsiManager.getInstance(getProject()!!).findFile(mainFile)!!
            val document = FileDocumentManager.getInstance()!!.getDocument(mainFile)!!
            val editor = EditorFactory.getInstance()!!.createEditor(document, getProject()!!)!!

            val caretOffset = extractCaretOffset(document)
            val elementAtCaret = if (caretOffset >= 0) {
                TargetElementUtilBase.getInstance()!!.findTargetElement(
                        editor,
                        TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED or TargetElementUtilBase.ELEMENT_NAME_ACCEPTED,
                        caretOffset
                )
            }
            else null

            try {
                action.runRefactoring(mainPsiFile, elementAtCaret, config)

                assert(!conflictFile.exists())
            }
            catch(e: ConflictsInTestsException) {
                JetTestUtils.assertEqualsToFile(conflictFile, e.getMessages().sort().makeString("\n"))
            }
            finally {
                PsiDocumentManager.getInstance(getProject()!!).commitAllDocuments()
                FileDocumentManager.getInstance()?.saveAllDocuments()

                EditorFactory.getInstance()!!.releaseEditor(editor)
            }
        }
    }

    protected fun getTestDirName(lowercaseFirstLetter : Boolean) : String {
        val testName = getTestName(lowercaseFirstLetter)
        return testName.substring(0, testName.lastIndexOf('_')).replace('_', '/')
    }

    protected fun doTest(action : (VirtualFile, VirtualFile?) -> Unit) {
        super.doTest(action, getTestDirName(true))
    }

    protected override fun getTestRoot() : String {
        return "/refactoring/move/"
    }

    protected override fun getTestDataPath() : String {
        return PluginTestCaseBase.getTestDataPathBase()
    }
}

fun JsonObject.getString(name: String): String {
    val member = getNullableString(name)
    if (member == null) {
        throw IllegalStateException("Member with name '$name' is expected in '$this'")
    }

    return member
}

fun JsonObject.getNullableString(name: String): String? = this[name]?.getAsString()

enum class MoveAction {
    abstract fun runRefactoring(mainFile: PsiFile, elementAtCaret: PsiElement?, config: JsonObject)
}
