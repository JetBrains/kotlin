/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.perf.UltraLightChecker
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

abstract class AbstractUltraLightClassLoadingTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun doTest(testDataPath: String) {
        val file = myFixture.addFileToProject(testDataPath, File(testDataPath).readText()) as KtFile
        for (ktClass in UltraLightChecker.allClasses(file)) {
            val clsLoadingExpected = ktClass.docComment?.text?.contains("should load cls") == true
            val ultraLightClass = UltraLightChecker.checkClassEquivalence(ktClass)
            if (ultraLightClass != null) {
                assertEquals(
                    "Cls-loaded status differs from expected for ${ultraLightClass.qualifiedName}",
                    clsLoadingExpected,
                    ultraLightClass.isClsDelegateLoaded
                )
            }
        }

    }
}