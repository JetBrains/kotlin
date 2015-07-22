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
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.util.ArrayUtil
import com.intellij.util.Function
import com.intellij.util.containers.ContainerUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import java.io.File

public abstract class AbstractKotlinSourceInJavaCompletionTest : JetFixtureCompletionBaseTestCase() {
    override fun getPlatform() = JvmPlatform

    override fun doTest(testPath: String) {
        val mockLibDir = File(COMPLETION_TEST_DATA_BASE_PATH + "/injava/mockLib")
        val listFiles = mockLibDir.listFiles()
        TestCase.assertNotNull(listFiles)
        val paths = ArrayUtil.toStringArray(ContainerUtil.map<File, String>(listFiles, object : Function<File, String> {
            override fun `fun`(file: File): String {
                return FileUtil.toSystemIndependentName(file.getAbsolutePath())
            }
        }))
        myFixture.configureByFiles(*paths)
        super.doTest(testPath)
    }

    override fun getProjectDescriptor() = LightCodeInsightFixtureTestCase.JAVA_LATEST

    override fun complete(invocationCount: Int) = myFixture.complete(CompletionType.BASIC, invocationCount)
}
