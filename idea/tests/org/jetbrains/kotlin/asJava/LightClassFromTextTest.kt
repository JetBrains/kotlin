/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory

// see KtFileLightClassTest
class LightClassFromTextTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun testSimple() {
        myFixture.configureByText("Dummy.kt", "") as KtFile
        val classes = classesFromText("class C {}\nobject O {}")
        assertEquals(2, classes.size)
        assertEquals("C", classes[0].qualifiedName)
        assertEquals("O", classes[1].qualifiedName)
    }

    fun testFileClass() {
        myFixture.configureByText("A.kt", "fun f() {}") as KtFile
        val classes = classesFromText("fun g() {}", fileName = "A.kt")

        assertEquals(1, classes.size)
        val facadeClass = classes.single()
        assertEquals("AKt", facadeClass.qualifiedName)

        val gMethods = facadeClass.findMethodsByName("g", false)
        assertEquals(1, gMethods.size)
        assertEquals(PsiType.VOID, gMethods.single().returnType)

        val fMethods = facadeClass.findMethodsByName("f", false)
        assertEquals(0, fMethods.size)
    }

    fun testMultifileClass() {
        myFixture.configureByFiles("multifile1.kt", "multifile2.kt")

        val facadeClass = classesFromText("""
        @file:kotlin.jvm.JvmMultifileClass
        @file:kotlin.jvm.JvmName("Foo")

        fun jar() {
        }

        fun boo() {
        }
         """).single()

        assertEquals(1, facadeClass.findMethodsByName("jar", false).size)
        assertEquals(1, facadeClass.findMethodsByName("boo", false).size)
        assertEquals(0, facadeClass.findMethodsByName("bar", false).size)
        assertEquals(0, facadeClass.findMethodsByName("foo", false).size)
    }

    fun testReferenceToOuterContext() {
        val contextFile = myFixture.configureByText("Example.kt", "package foo\n class Example") as KtFile

        val syntheticClass = classesFromText("""
        package bar

        import foo.Example

        class Usage {
            fun f(): Example = Example()
        }
         """).single()

        val exampleClass = contextFile.classes.single()
        assertEquals("Example", exampleClass.name)

        val f = syntheticClass.findMethodsByName("f", false).single()
        assertEquals(exampleClass, (f.returnType as PsiClassType).resolve())
    }

    fun testHeaderDeclarations() {
        val contextFile = myFixture.configureByText("Header.kt", "header class Foo\n\nheader fun foo()\n") as KtFile
        val headerClass = contextFile.declarations.single { it is KtClassOrObject }
        assertEquals(0, headerClass.toLightElements().size)
        val headerFunction = contextFile.declarations.single { it is KtNamedFunction }
        assertEquals(0, headerFunction.toLightElements().size)
    }

    private fun classesFromText(text: String, fileName: String = "A.kt"): Array<out PsiClass> {
        val file = KtPsiFactory(project).createFileWithLightClassSupport(fileName, text, myFixture.file)
        val classes = file.classes
        return classes
    }

    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/asJava/fileLightClass/"
    }
}