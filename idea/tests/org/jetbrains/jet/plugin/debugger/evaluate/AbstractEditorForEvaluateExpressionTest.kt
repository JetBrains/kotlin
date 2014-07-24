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

package org.jetbrains.jet.plugin.debugger.evaluate

import org.jetbrains.jet.checkers.AbstractJetPsiCheckerTest
import org.jetbrains.jet.completion.AbstractJvmBasicCompletionTest
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.psi.PsiElement
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.InTextDirectivesUtils
import org.jetbrains.jet.lang.psi.JetCodeFragment

abstract class AbstractCodeFragmentHighlightingTest : AbstractJetPsiCheckerTest() {
    override fun doTest(filePath: String) {
        myFixture.configureByCodeFragment(filePath)
        myFixture.checkHighlighting(true, false, false)
    }

    fun doTestWithImport(filePath: String) {
        myFixture.configureByCodeFragment(filePath)

        ApplicationManager.getApplication()?.runWriteAction {
            val fileText = FileUtil.loadFile(File(filePath), true)
            InTextDirectivesUtils.findListWithPrefixes(fileText, "// IMPORT: ").forEach {
                ImportInsertHelper.addImportDirectiveIfNeeded(FqName(it), (myFixture.getFile() as JetFile))
            }
        }

        myFixture.checkHighlighting(true, false, false)
    }
}

abstract class AbstractCodeFragmentCompletionTest : AbstractJvmBasicCompletionTest() {
    override fun setUpFixture(testPath: String) {
        myFixture.configureByCodeFragment(testPath)
    }
}

private fun JavaCodeInsightTestFixture.configureByCodeFragment(filePath: String) {
    configureByFile(filePath)

    val elementAt = getFile()?.findElementAt(getCaretOffset())
    val file = createCodeFragment(filePath, elementAt!!)

    configureFromExistingVirtualFile(file.getVirtualFile()!!)
}

private fun createCodeFragment(filePath: String, contextElement: PsiElement): JetCodeFragment {
    val fileForFragment = File(filePath + ".fragment")
    val codeFragmentText = FileUtil.loadFile(fileForFragment, true).trim()
    val psiFactory = JetPsiFactory(contextElement.getProject())
    if (fileForFragment.readLines().size == 1) {
        return psiFactory.createExpressionCodeFragment(
                codeFragmentText,
                KotlinCodeFragmentFactory.getContextElement(contextElement)
        )
    }
    return psiFactory.createBlockCodeFragment(
            codeFragmentText,
            KotlinCodeFragmentFactory.getContextElement(contextElement)
    )
}