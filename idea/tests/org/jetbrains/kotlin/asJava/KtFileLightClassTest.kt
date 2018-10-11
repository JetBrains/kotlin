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

package org.jetbrains.kotlin.asJava

import com.intellij.injected.editor.EditorWindow
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

class KtFileLightClassTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinLightProjectDescriptor.INSTANCE

    fun testSimple() {
        val file = myFixture.configureByText("A.kt", "class C {}\nobject O {}") as KtFile
        val classes = file.classes
        assertEquals(2, classes.size)
        assertEquals("C", classes[0].qualifiedName)
        assertEquals("O", classes[1].qualifiedName)
    }

    fun testFileClass() {
        val file = myFixture.configureByText("A.kt", "fun f() {}") as KtFile
        val classes = file.classes
        assertEquals(1, classes.size)
        assertEquals("AKt", classes[0].qualifiedName)
    }

    fun testMultifileClass() {
        val file = myFixture.configureByFiles("multifile1.kt", "multifile2.kt")[0] as KtFile
        val aClass = file.classes.single()
        assertEquals(1, aClass.findMethodsByName("foo", false).size)
        assertEquals(1, aClass.findMethodsByName("bar", false).size)
    }

    fun testAliasesOnly() {
        val file = myFixture.configureByFile("aliasesOnly.kt") as KtFile
        val aClass = file.classes.single()
        assertEquals(0, aClass.getMethods().size)
    }

    fun testNoFacadeForScript() {
        val file = myFixture.configureByText("foo.kts", "package foo") as KtFile
        assertEquals(0, file.classes.size)
        val facadeFiles = KotlinAsJavaSupport.getInstance(project).findFilesForFacade(FqName("foo.FooKt"), GlobalSearchScope.allScope(project))
        assertEquals(0, facadeFiles.size)
    }

    fun testNoFacadeForHeaderClass() {
        val file = myFixture.configureByText("foo.kt", "header fun foo(): Int") as KtFile
        assertEquals(0, file.classes.size)
        val facadeFiles = KotlinAsJavaSupport.getInstance(project).findFilesForFacade(FqName("foo.FooKt"), GlobalSearchScope.allScope(project))
        assertEquals(0, facadeFiles.size)
    }

    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/asJava/fileLightClass/"
    }

    fun testInjectedCode() {
        myFixture.configureByText("foo.kt", """
            import org.intellij.lang.annotations.Language

            fun foo(@Language("kotlin") a: String){a.toString()}

            fun bar(){ foo("class<caret> A") }
            """)


        myFixture.testHighlighting("foo.kt")

        val injectedFile = (editor as? EditorWindow)?.injectedFile
        assertEquals("Wrong injection language", "kotlin", injectedFile?.language?.id)
        assertEquals("Injected class should be `A`", "A", ((injectedFile as KtFile).declarations.single() as KtClass).toLightClass()!!.name)
    }


    fun testSameVirtualFileForLightElement() {

        val psiFile = myFixture.addFileToProject(
            "pkg/AnnotatedClass.kt", """
            package pkg

            class AnnotatedClass {
                    @Deprecated("a")
                    fun bar(param: String) = null
            }
        """.trimIndent()
        )

        fun lightElement(file: PsiFile): PsiElement = (file as KtFile).classes.single()
            .methods.first { it.name == "bar" }
            .annotations.first { it.qualifiedName == "kotlin.Deprecated" }.also {
            // Otherwise following asserts have no sense
            TestCase.assertTrue("psi element should be light ", it is KtLightElement<*, *>)
        }


        val copied = psiFile.copied()
        TestCase.assertNull("virtual file for copy should be null", copied.virtualFile)
        TestCase.assertNotNull("psi element in copy", lightElement(copied))
        TestCase.assertSame("copy.originalFile should be psiFile", copied.originalFile, psiFile)

        // virtual files should be the same for light and non-light element,
        // otherwise we will not be able to find proper module by file from light element
        TestCase.assertSame(
            "virtualFiles of element and file itself should be the same",
            lightElement(copied).containingFile.originalFile.virtualFile,
            copied.originalFile.virtualFile
        )
    }

}
