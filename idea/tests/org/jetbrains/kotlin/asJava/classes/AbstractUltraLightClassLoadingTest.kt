/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.PsiClassRenderer.renderClass
import org.jetbrains.kotlin.idea.perf.UltraLightChecker
import org.jetbrains.kotlin.idea.perf.UltraLightChecker.checkDescriptorsLeak
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractUltraLightClassLoadingTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun doTest(testDataPath: String) {
        val sourceText = File(testDataPath).readText()
        val file = myFixture.addFileToProject(testDataPath, sourceText) as KtFile

        UltraLightChecker.checkForReleaseCoroutine(sourceText, module)

        val expectedTextFile = File(testDataPath.replaceFirst("\\.kt\$".toRegex(), ".java"))
        if (expectedTextFile.exists()) {
            val renderedResult =
                UltraLightChecker.allClasses(file).mapNotNull { ktClass ->
                    LightClassGenerationSupport.getInstance(ktClass.project).createUltraLightClass(ktClass)?.let { it to ktClass }
                }.joinToString("\n\n") { (ultraLightClass, ktClass) ->
                    with(UltraLightChecker) {
                        ultraLightClass.renderClass().also {
                            checkDescriptorsLeak(ultraLightClass)
                        }
                    }
                }

            KotlinTestUtils.assertEqualsToFile(expectedTextFile, renderedResult)
            return
        }

        for (ktClass in UltraLightChecker.allClasses(file)) {
            val ultraLightClass = UltraLightChecker.checkClassEquivalence(ktClass)
            if (ultraLightClass != null) {
                checkDescriptorsLeak(ultraLightClass)
            }
        }

    }
}
