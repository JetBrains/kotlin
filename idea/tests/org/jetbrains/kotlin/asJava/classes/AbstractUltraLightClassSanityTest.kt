/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.perf.UltraLightChecker
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

abstract class AbstractUltraLightClassSanityTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun doTest(testDataPath: String) {
        val ioFile = File(testDataPath)
        if (ioFile.name == "AllOpenAnnotatedClasses.kt") {
            return //tests allopen compiler plugin that we don't have in this test
        }

        val file = myFixture.addFileToProject(testDataPath, ioFile.readText()) as KtFile
        UltraLightChecker.checkClassEquivalence(file)
    }
}