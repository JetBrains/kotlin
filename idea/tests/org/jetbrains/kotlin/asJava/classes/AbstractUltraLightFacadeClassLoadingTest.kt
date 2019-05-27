/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.idea.perf.UltraLightChecker
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import java.io.File

abstract class AbstractUltraLightFacadeClassTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun doTest(testDataPath: String) {
        val file = myFixture.addFileToProject(testDataPath, File(testDataPath).readText()) as KtFile

        val additionalFilePath = "$testDataPath.1"
        if(File(additionalFilePath).exists()) {
            myFixture.addFileToProject(additionalFilePath.replaceFirst(".kt.1", "1.kt"), File(additionalFilePath).readText())
        }

        val scope = GlobalSearchScope.allScope(project)
        val facades = KotlinAsJavaSupport.getInstance(project).getFacadeNames(FqName.ROOT, scope)

        for (facadeName in facades) {
            val ultraLightClass = UltraLightChecker.checkFacadeEquivalence(FqName(facadeName), scope, project)
            if (ultraLightClass != null) {
                checkClassLoadingExpectations(file, ultraLightClass)
            }
        }
    }

    private fun checkClassLoadingExpectations(
        primaryFile: KtFile,
        ultraLightClass: KtLightClassForFacade
    ) {

         val clsLoadingExpected = primaryFile.findDescendantOfType<KDoc> { it.text?.contains("should load cls") == true } !== null

        assertEquals(
            "Cls-loaded status differs from expected for ${ultraLightClass.qualifiedName}",
            clsLoadingExpected,
            ultraLightClass.isClsDelegateLoaded
        )
    }
}
