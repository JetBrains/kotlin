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

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.name.FqName
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
        val facadeFiles = LightClassGenerationSupport.getInstance(project).findFilesForFacade(FqName("foo.FooKt"), GlobalSearchScope.allScope(project))
        assertEquals(0, facadeFiles.size)
    }

    fun testNoFacadeForHeaderClass() {
        val file = myFixture.configureByText("foo.kt", "header fun foo(): Int") as KtFile
        assertEquals(0, file.classes.size)
        val facadeFiles = LightClassGenerationSupport.getInstance(project).findFilesForFacade(FqName("foo.FooKt"), GlobalSearchScope.allScope(project))
        assertEquals(0, facadeFiles.size)
    }

    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/asJava/fileLightClass/"
    }
}
