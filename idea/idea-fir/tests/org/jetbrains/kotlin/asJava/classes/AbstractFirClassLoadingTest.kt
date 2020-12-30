/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.findUsages.doTestWithFIRFlagsByPath
import org.jetbrains.kotlin.idea.perf.UltraLightChecker
import org.jetbrains.kotlin.idea.perf.UltraLightChecker.getJavaFileForTest
import org.jetbrains.kotlin.idea.perf.UltraLightChecker.renderLightClasses
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractFirClassLoadingTest : AbstractUltraLightClassLoadingTest() {

    override fun isFirPlugin(): Boolean = true

    override fun doTest(testDataPath: String) = doTestWithFIRFlagsByPath(testDataPath) {
        doTestImpl(testDataPath)
    }

    private fun doTestImpl(testDataPath: String) {

        val testDataFile = File(testDataPath)
        val sourceText = testDataFile.readText()
        val file = myFixture.addFileToProject(testDataPath, sourceText) as KtFile

        val classFabric = KotlinAsJavaSupport.getInstance(project)

        val expectedTextFile = getJavaFileForTest(testDataPath)

        val renderedClasses = executeOnPooledThreadInReadAction {
            val lightClasses = UltraLightChecker.allClasses(file).mapNotNull { classFabric.getLightClass(it) }
            renderLightClasses(testDataPath, lightClasses)
        }!!

        KotlinTestUtils.assertEqualsToFile(expectedTextFile, renderedClasses)
    }
}
