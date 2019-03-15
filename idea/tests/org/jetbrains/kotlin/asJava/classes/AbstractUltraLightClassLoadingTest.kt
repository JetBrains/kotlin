/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.idea.perf.UltraLightChecker
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractUltraLightClassLoadingTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun doTest(testDataPath: String) {
        val file = myFixture.addFileToProject(testDataPath, File(testDataPath).readText()) as KtFile

        val expectedTextFile = File(testDataPath.replaceFirst("\\.kt\$".toRegex(), ".java"))
        if (expectedTextFile.exists()) {
            val renderedResult =
                UltraLightChecker.allClasses(file).mapNotNull { ktClass ->
                    LightClassGenerationSupport.getInstance(ktClass.project).createUltraLightClass(ktClass)?.let { it to ktClass }
                }.joinToString("\n\n") { (ultraLightClass, ktClass) ->
                    with(UltraLightChecker) {
                        ultraLightClass.renderClass().also {
                            checkClassLoadingExpectations(ktClass, ultraLightClass)
                        }
                    }
                }

            KotlinTestUtils.assertEqualsToFile(expectedTextFile, renderedResult)
            return
        }

        for (ktClass in UltraLightChecker.allClasses(file)) {
            val ultraLightClass = UltraLightChecker.checkClassEquivalence(ktClass)
            if (ultraLightClass != null) {
                checkClassLoadingExpectations(ktClass, ultraLightClass)
            }
        }

    }

    private fun checkClassLoadingExpectations(
        ktClass: KtClassOrObject,
        ultraLightClass: KtUltraLightClass
    ) {
        val clsLoadingExpected = ktClass.docComment?.text?.contains("should load cls") == true
        assertEquals(
            "Cls-loaded status differs from expected for ${ultraLightClass.qualifiedName}",
            clsLoadingExpected,
            ultraLightClass.isClsDelegateLoaded
        )
    }
}
