/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms

abstract class AbstractJvmWithLibBasicCompletionTest : KotlinFixtureCompletionBaseTestCase() {
    private val TEST_PATH = COMPLETION_TEST_DATA_BASE_PATH + "/basic/withLib"

    override fun getProjectDescriptor(): LightProjectDescriptor {
        if (PluginTestCaseBase.isAllFilesPresentTest(getTestName(true))) {
            return super.getProjectDescriptor()
        }
        return SdkAndMockLibraryProjectDescriptor(TEST_PATH + "/" + getTestName(false) + "Src", false)
    }

    override fun getPlatform() = DefaultBuiltInPlatforms.jvmPlatform
    override fun defaultCompletionType() = CompletionType.BASIC
}
