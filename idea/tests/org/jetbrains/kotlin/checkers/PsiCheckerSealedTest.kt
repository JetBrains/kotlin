/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.runner.RunWith

@TestMetadata("idea/testData/checker/sealed")
@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class PsiCheckerSealedTest : AbstractPsiCheckerTest() {

    fun testOutsideOfPackageInheritors() {
        doTest(
            "SealedOutsidePackageInheritors.kt", // opened in the test editor
            "SealedDeclaration.kt"
        )
    }

    fun testWhenExhaustiveness() {
        doTest(
            "SealedInheritors.kt", // opened in the test editor
            "SealedDeclaration.kt"
        )
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = getProjectDescriptorFromTestName()
}