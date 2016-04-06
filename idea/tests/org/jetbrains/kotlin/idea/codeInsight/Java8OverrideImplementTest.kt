/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.codeInsight

import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase

class Java8OverrideImplementTest : AbstractOverrideImplementTest() {
    companion object {
        private val TEST_PATH = PluginTestCaseBase.getTestDataPathBase() + "/codeInsight/overrideImplement/jdk8"
    }

    override fun setUp() {
        super.setUp()
        myFixture.testDataPath = TEST_PATH
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK

    fun testOverrideCollectionStream() {
        doOverrideFileTest("stream")
    }
}
