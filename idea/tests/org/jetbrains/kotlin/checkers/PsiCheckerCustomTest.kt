/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import com.intellij.codeInspection.ex.EntryPointsManagerBase
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.test.KotlinTestUtils

class PsiCheckerCustomTest : AbstractPsiCheckerTest() {
    fun testNoUnusedParameterWhenCustom() {
        val testAnnotation = "MyTestAnnotation"
        EntryPointsManagerBase.getInstance(project).ADDITIONAL_ANNOTATIONS.add(testAnnotation)
        try {
            doTest(getTestDataFile("noUnusedParameterWhenCustom.kt"))
        }
        finally {
            EntryPointsManagerBase.getInstance(project).ADDITIONAL_ANNOTATIONS.remove(testAnnotation)
        }
    }

    fun testConflictingOverloadsMultifile1() {
        doTest(getTestDataFile("conflictingOverloadsMultifile1a.kt"),
               getTestDataFile("conflictingOverloadsMultifile1b.kt"))
    }

    fun testConflictingOverloadsMultifile2() {
        doTest(getTestDataFile("conflictingOverloadsMultifile2a.kt"),
               getTestDataFile("conflictingOverloadsMultifile2b.kt"))
    }

    private fun getTestDataFile(localName: String) = "idea/testData/checker/custom/$localName"

    override fun getTestDataPath(): String = KotlinTestUtils.getHomeDirectory()

    override fun getProjectDescriptor(): LightProjectDescriptor = getProjectDescriptorFromTestName()
}