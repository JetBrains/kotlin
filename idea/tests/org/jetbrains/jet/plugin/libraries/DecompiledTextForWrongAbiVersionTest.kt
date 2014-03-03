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

package org.jetbrains.jet.plugin.libraries

import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.jet.JetTestCaseBuilder
import com.intellij.psi.PsiManager
import junit.framework.Assert
import com.intellij.psi.PsiCompiledFile
import org.jetbrains.jet.plugin.JetJdkAndLibraryProjectDescriptor
import java.io.File
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.impl.compiled.ClsFileImpl

public class DecompiledTextForWrongAbiVersionTest : JetLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return JetJdkAndLibraryProjectDescriptor(File(JetTestCaseBuilder.getTestDataPathBase() + "/cli/jvm/wrongAbiVersionLib"))
    }

    fun testClassWithWrongAbiVersion() {
        val root = NavigateToDecompiledLibraryTest.findTestLibraryRoot(myModule!!)
        checkFileWithWrongAbiVersion(root!!.findChild("ClassWithWrongAbiVersion.class")!!)
    }

    fun testPackageFacadeWithWrongAbiVersion() {
        val root = NavigateToDecompiledLibraryTest.findTestLibraryRoot(myModule!!)
        checkFileWithWrongAbiVersion(root!!.findChild("wrong")!!.findChild("WrongPackage.class")!!)
    }

    private fun checkFileWithWrongAbiVersion(file: VirtualFile) {
        val psiFile = PsiManager.getInstance(getProject()!!).findFile(file)
        Assert.assertTrue(psiFile is ClsFileImpl)
        val decompiledPsiFile = (psiFile as PsiCompiledFile).getDecompiledPsiFile()
        Assert.assertTrue(decompiledPsiFile is PsiJavaFile)
        val decompiledText = decompiledPsiFile!!.getText()!!
        Assert.assertTrue(decompiledText.contains(INCOMPATIBLE_ABI_VERSION_COMMENT))
        Assert.assertTrue((decompiledPsiFile as PsiJavaFile).getClasses().size == 1)
    }
}