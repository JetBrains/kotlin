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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.LightClassTestCommon
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.idea.KotlinDaemonAnalyzerTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.junit.Assert
import java.io.File

abstract class AbstractIdeLightClassTest : KotlinLightCodeInsightFixtureTestCase() {
    fun doTest(testDataPath: String) {
        myFixture.configureByFile(testDataPath)
        testLightClass(project, myFixture.file as KtFile, testDataPath, { LightClassTestCommon.removeEmptyDefaultImpls(it) })
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}

abstract class AbstractIdeCompiledLightClassTest : KotlinDaemonAnalyzerTestCase() {
    override fun setUp() {
        super.setUp()

        val testName = getTestName(false)
        if (KotlinTestUtils.isAllFilesPresentTest(testName)) return

        val filePath = "${KotlinTestUtils.getTestsRoot(this.javaClass)}/${getTestName(false)}.kt"

        Assert.assertTrue("File doesn't exist $filePath", File(filePath).exists())

        val libraryJar = MockLibraryUtil.compileLibraryToJar(filePath, libName(), false, false, false)
        val jarUrl = "jar://" + FileUtilRt.toSystemIndependentName(libraryJar.absolutePath) + "!/"
        ModuleRootModificationUtil.addModuleLibrary(module, jarUrl)
    }

    private fun libName() = "libFor" + getTestName(false)

    fun doTest(testDataPath: String) {
        testLightClass(project, null, testDataPath, { it })
    }
}

private fun testLightClass(project: Project, ktFile: KtFile?, testDataPath: String, normalize: (String) -> String) {
    LightClassTestCommon.testLightClass(
            File(testDataPath),
            findLightClass = { fqName ->
                var clazz: PsiClass? = JavaPsiFacade.getInstance(project).findClass(fqName, GlobalSearchScope.allScope(project))
                if (clazz == null) {
                    clazz = PsiTreeUtil.findChildrenOfType(ktFile, KtClassOrObject::class.java)
                            .find { fqName.endsWith(it.nameAsName!!.asString()) }
                            ?.let { KtLightClassForSourceDeclaration.create(it) }
                }
                if (clazz != null) {
                    PsiElementChecker.checkPsiElementStructure(clazz)
                }
                clazz

            },
            normalizeText = { text ->
                //NOTE: ide and compiler differ in names generated for parameters with unspecified names
                text
                        .replace("java.lang.String s,", "java.lang.String p,")
                        .replace("java.lang.String s)", "java.lang.String p)")
                        .replace("java.lang.String s1", "java.lang.String p1")
                        .replace("java.lang.String s2", "java.lang.String p2")
                        .removeLinesStartingWith("@" + JvmAnnotationNames.METADATA_FQ_NAME.asString())
                        .run(normalize)
            }
    )
}

private fun String.removeLinesStartingWith(prefix: String): String {
    return lines().filterNot { it.trimStart().startsWith(prefix) }.joinToString(separator = "\n")
}
