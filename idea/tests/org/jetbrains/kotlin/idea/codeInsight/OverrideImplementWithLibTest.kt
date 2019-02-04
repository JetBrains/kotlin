/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase

class OverrideImplementWithLibTest : AbstractOverrideImplementTest() {
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
