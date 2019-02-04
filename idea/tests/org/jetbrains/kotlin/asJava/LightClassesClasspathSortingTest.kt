/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.impl.ResolveScopeManager
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.idea.caches.lightClasses.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import kotlin.test.assertNotNull

class LightClassesClasspathSortingTest : KotlinLightCodeInsightFixtureTestCase() {

    fun testExplicitClass() {
        doTest("test1.A")
    }

    fun testFileClass() {
        doTest("test2.FileKt")
    }

    private fun doTest(fqName: String) {
        // Same classes are in sources and in compiled Kotlin library. Test that light classes from sources have a priority.

        val dirName = getTestName(true)

        val testDirRoot = File(testDataPath)
        val filePaths = File(testDirRoot, dirName).listFiles().map { it.toRelativeString(testDirRoot) }.toTypedArray()
        myFixture.configureByFiles(*filePaths)

        checkLightClassBeforeDecompiled(fqName)
    }

    private fun checkLightClassBeforeDecompiled(fqName: String) {
        val psiClass = JavaPsiFacade.getInstance(project).findClass(fqName, ResolveScopeManager.getElementResolveScope(file))

        assertNotNull(psiClass, "Can't find class for $fqName")
        psiClass!!
        assert(psiClass is KtLightClassForSourceDeclaration || psiClass is KtLightClassForFacade) { "Should be an explicit light class, but was $fqName ${psiClass::class.java}" }
        assert(psiClass !is KtLightClassForDecompiledDeclaration) { "Should not be decompiled light class: $fqName ${psiClass::class.java}" }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return SdkAndMockLibraryProjectDescriptor(
            "$testDataPath${getTestName(true)}",
            true
        )
    }

    override fun getTestDataPath(): String {
        return KotlinTestUtils.getHomeDirectory() + "/idea/testData/decompiler/lightClassesOrder/"
    }
}