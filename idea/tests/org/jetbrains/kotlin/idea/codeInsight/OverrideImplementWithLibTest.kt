/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.codeInsight.generation.ClassMember
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class OldOverrideImplementWithLibTest : OverrideImplementWithLibTest<OverrideMemberChooserObject>(), OldOverrideImplementTestMixIn

abstract class OverrideImplementWithLibTest<T : ClassMember> : AbstractOverrideImplementTest<T>() {
    private val TEST_PATH = PluginTestCaseBase.getTestDataPathBase() + "/codeInsight/overrideImplement/withLib"

    override fun setUp() {
        super.setUp()
        myFixture.testDataPath = TEST_PATH
    }

    override fun getProjectDescriptor() =
        SdkAndMockLibraryProjectDescriptor(TEST_PATH + "/" + getTestName(true) + "Src", false)

    fun testFakeOverride() {
        doOverrideFileTest()
    }

    fun testGenericSubstituted() {
        doOverrideFileTest()
    }
}
