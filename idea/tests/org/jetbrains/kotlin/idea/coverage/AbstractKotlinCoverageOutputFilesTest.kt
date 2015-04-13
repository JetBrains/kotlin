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

package org.jetbrains.kotlin.idea.coverage

import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.test.JetTestUtils
import java.io.File
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.util.application.runWriteAction

public abstract class AbstractKotlinCoverageOutputFilesTest(): JetLightCodeInsightFixtureTestCase() {
    private val TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/coverage/outputFiles"

    override fun getTestDataPath(): String? = TEST_DATA_PATH

    public fun doTest(path: String) {
        val kotlinFile = myFixture.configureByFile(path) as JetFile
        val outDir = myFixture.getTempDirFixture().findOrCreateDir("coverageTestOut")
        try {
            FileUtil.loadLines(File(path.replace(".kt", ".classes.txt"))).forEach {
                runWriteAction {
                    createEmptyFile(outDir, it)
                }
            }

            val actualClasses = KotlinCoverageExtension.collectGeneratedClassQualifiedNames(outDir, kotlinFile)
            JetTestUtils.assertEqualsToFile(File(path.replace(".kt", ".expected.txt")), actualClasses!!.join("\n"))
        }
        finally {
            runWriteAction {
                outDir.delete(null)
            }
        }
    }
}

private fun createEmptyFile(dir: VirtualFile, relativePath: String) {
    var currentDir = dir
    val segments = relativePath.split('/')
    segments.forEachIndexed { i, s ->
        if (i < segments.size() - 1) {
            currentDir = currentDir.createChildDirectory(null, s)
        } else {
            currentDir.createChildData(null, s)
        }
    }
}
