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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.apache.commons.io.FileUtils
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import java.io.File

abstract class AbstractKotlinSourceInJavaCompletionTest : KotlinFixtureCompletionBaseTestCase() {
    override fun getPlatform() = JvmPlatform

    override fun doTest(testPath: String) {
        val mockPath = RELATIVE_COMPLETION_TEST_DATA_BASE_PATH + "/injava/mockLib"
        val mockLibDir = File(mockPath)
        fun collectPaths(dir: File): List<String> {
            return dir.listFiles()!!.flatMap {
                if (it.isDirectory) {
                    collectPaths(it)
                } else listOf(FileUtil.toSystemIndependentName(it.path))
            }
        }

        val paths = collectPaths(mockLibDir).toTypedArray()
        paths.forEach { path ->
            val vFile = myFixture.copyFileToProject(path, path.substring(mockPath.length))
            myFixture.configureFromExistingVirtualFile(vFile)
        }

        LightClassComputationControl.testWithControl(project, FileUtil.loadFile(File(testPath))) {
            super.doTest(testPath)
        }
    }

    override fun getProjectDescriptor() = LightCodeInsightFixtureTestCase.JAVA_LATEST
    override fun defaultCompletionType() = CompletionType.BASIC
}