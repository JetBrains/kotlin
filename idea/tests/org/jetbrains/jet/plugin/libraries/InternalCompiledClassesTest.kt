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
import org.jetbrains.jet.plugin.JdkAndMockLibraryProjectDescriptor
import org.jetbrains.jet.plugin.PluginTestCaseBase
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jet.lang.resolve.java.JvmAbi
import com.intellij.psi.PsiManager
import junit.framework.Assert
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils
import com.intellij.psi.ClassFileViewProvider
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.PsiCompiledFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import org.jetbrains.jet.lang.resolve.name.FqName

//TODO: test for local functions and classes
public class InternalCompiledClassesTest : JetLightCodeInsightFixtureTestCase() {

    private val TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/libraries/internalClasses"

    // Visible in idea-13-0
    fun testPackagePartIsInvisible() {
        val project = getProject()!!
        doTest("package part class", acceptFile = { getNameWithoutExtension().contains(PackageClassUtils.PACKAGE_CLASS_NAME_SUFFIX + "-") }) {
            val psiFile = PsiManager.getInstance(project).findFile(this)

            Assert.assertEquals(
                    "// IntelliJ API Decompiler stub source generated from a class file\n" +
                    "// Implementation of methods is not available",
                    psiFile?.getText()?.trim()?.replaceAll("  //", "//")
            )
        }
    }

    fun testAnonymousFunctionIsInvisible() = doTestNoPsiFilesAreBuiltFor("anonymous function") {
        isAnonymousFunction(this)
    }

    fun testInnerClassIsInvisible() = doTestNoPsiFilesAreBuiltFor("inner or nested class") {
        ClassFileViewProvider.isInnerOrAnonymousClass(this)
    }

    fun testTraitImplClassIsVisibleAsJavaClass() = doTestNoPsiFilesAreBuiltFor("trait impl class") {
        getNameWithoutExtension().endsWith(JvmAbi.TRAIT_IMPL_SUFFIX)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return JdkAndMockLibraryProjectDescriptor(TEST_DATA_PATH, withSources = false)
    }

    private fun doTestNoPsiFilesAreBuiltFor(fileKind: String, acceptFile: VirtualFile.() -> Boolean) {
        val project = getProject()!!
        doTest(fileKind, acceptFile) {
            val psiFile = PsiManager.getInstance(project).findFile(this)
            Assert.assertNull("PSI files for $fileKind classes should not be build, is was build for: ${this.getPresentableName()}",
                              psiFile)

        }
    }

    private fun doTest(fileKind: String, acceptFile: VirtualFile.() -> Boolean, performTest: VirtualFile.() -> Unit) {
        val root = NavigateToDecompiledLibraryTest.findTestLibraryRoot(myModule!!)!!
        var foundAtLeastOneFile = false
        root.checkRecursively {
            if (acceptFile()) {
                foundAtLeastOneFile = true
                performTest()
            }
        }
        Assert.assertTrue("Should find at least one file of kind ($fileKind). This assertion can fail in following scenarios:\n" +
                          "1. Test data is bad and doesn't cover this case.\n2. ABI has changed and test no longer checks anything.",
                          foundAtLeastOneFile)
    }

    private fun VirtualFile.checkRecursively(body: VirtualFile.() -> Unit) {
        if (!isDirectory()) {
            body()
        }
        else {
            for (file in getChildren()!!) {
                file.checkRecursively(body)
            }
        }
    }
}
