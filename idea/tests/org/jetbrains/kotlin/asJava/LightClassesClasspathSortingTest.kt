/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.impl.ResolveScopeManager
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.idea.caches.resolve.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.test.JdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.configureAs
import java.io.File
import kotlin.test.assertNotNull

class LightClassesClasspathSortingTest : KotlinCodeInsightTestCase() {
    fun testExplicitClass() {
        doTest("test1.A")
    }

    fun testFileClass() {
        doTest("test2.FileKt")
    }

    private fun doTest(fqName: String) {
        // Same classes are in sources and in compiled Kotlin library. Test that light classes from sources have a priority.

        // Configure library first to make classes be indexed before correspondent classes from sources
        val dirName = getTestName(true)
        module.configureAs(getProjectDescriptor(dirName))

        val testDirRoot = File(testDataPath)
        val filePaths = File(testDirRoot, dirName).listFiles().map { it.toRelativeString(testDirRoot) }.toTypedArray()
        configureByFiles(null, *filePaths)

        checkLightClassBeforeDecompiled(fqName)
    }

    private fun checkLightClassBeforeDecompiled(fqName: String) {
        val psiClass = JavaPsiFacade.getInstance(project).findClass(fqName, ResolveScopeManager.getElementResolveScope(file))

        assertNotNull(psiClass, "Can't find class for $fqName")
        psiClass!!
        assert(psiClass is KtLightClassForSourceDeclaration || psiClass is KtLightClassForFacade) { "Should be an explicit light class, but was $fqName ${psiClass.javaClass}" }
        assert(psiClass !is KtLightClassForDecompiledDeclaration) { "Should not be decompiled light class: $fqName ${psiClass.javaClass}" }
    }

    private fun getProjectDescriptor(dir: String) =
            JdkAndMockLibraryProjectDescriptor(PluginTestCaseBase.getTestDataPathBase() + "/decompiler/lightClassesOrder/$dir", true)

    override fun getTestDataPath(): String? {
        return PluginTestCaseBase.getTestDataPathBase() + "/decompiler/lightClassesOrder/"
    }
}