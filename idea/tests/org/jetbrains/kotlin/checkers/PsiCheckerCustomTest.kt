/*
 * Copyright 2010-2015 JetBrains s.r.o.
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