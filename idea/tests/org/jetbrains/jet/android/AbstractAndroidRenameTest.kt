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

package org.jetbrains.jet.android

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.refactoring.rename.RenameProcessor
import org.jetbrains.jet.plugin.PluginTestCaseBase
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import kotlin.test.assertEquals
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetProperty

public abstract class AbstractAndroidRenameTest : KotlinAndroidTestCase() {

    private val NEW_NAME = "NEWNAME"
    private val OLD_NAME = "OLDNAME"

    public fun doTest(path: String) {
        val f = myFixture!!
        f.copyDirectoryToProject(getResDir()!!, "res")
        f.configureByFile(path + getTestName(true) + ".kt")
        val editor = f.getEditor()
        val file = f.getFile()
        val completionEditor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file)
        val element = TargetElementUtilBase.findTargetElement(completionEditor, TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED or TargetElementUtilBase.ELEMENT_NAME_ACCEPTED)
        assert(element != null)

        // rename xml attribute by property
        renameElementWithTextOccurences(element!!, NEW_NAME)
        val resolved = GotoDeclarationAction.findTargetElement(f.getProject(), f.getEditor(), f.getCaretOffset())
        assertEquals("\"@+id/$NEW_NAME\"", resolved?.getText())

        // rename property by attribute
        renameElementWithTextOccurences(resolved!!, "@+id/$OLD_NAME")
        assertEquals(OLD_NAME, (f.getElementAtCaret() as JetProperty).getName())
    }

    private fun renameElementWithTextOccurences(element: PsiElement, newName: String) {
        val f = myFixture!!
        object : WriteCommandAction.Simple<Unit>(f.getProject()) {
            protected override fun run() {
                val editor = f.getEditor()
                val substitution = RenamePsiElementProcessor.forElement(element).substituteElementToRename(element, editor)
                RenameProcessor(f.getProject(), substitution, newName, false, true).run()
            }
        }.execute().throwException()
    }

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/android/rename/" + getTestName(true) + "/"
}


