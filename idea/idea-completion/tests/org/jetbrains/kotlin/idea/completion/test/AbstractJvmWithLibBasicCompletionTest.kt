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

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.JdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

abstract class AbstractJvmWithLibBasicCompletionTest : KotlinFixtureCompletionBaseTestCase() {
    private val TEST_PATH = COMPLETION_TEST_DATA_BASE_PATH + "/basic/withLib"

    override fun getProjectDescriptor(): LightProjectDescriptor {
        if (PluginTestCaseBase.isAllFilesPresentTest(getTestName(true))) {
            return super.getProjectDescriptor()
        }
        return JdkAndMockLibraryProjectDescriptor(TEST_PATH + "/" + getTestName(false) + "Src", false)
    }

    override fun getPlatform() = JvmPlatform
    override fun defaultCompletionType() = CompletionType.BASIC
}
