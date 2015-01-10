/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.coverage

import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase
import org.jetbrains.jet.plugin.PluginTestCaseBase
import org.jetbrains.kotlin.psi.JetFile
import kotlin.test.assertNotNull
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import kotlin.test.assertEquals
import java.io.File
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.jet.JetTestUtils

public abstract class AbstractKotlinCoverageOutputFilesTest(): JetLightCodeInsightFixtureTestCase() {
    private val TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/coverage/outputFiles"

    override fun getTestDataPath(): String? = TEST_DATA_PATH

    public fun doTest(path: String) {
        val kotlinFile = myFixture.configureByFile(path) as JetFile
        val actualClasses = KotlinCoverageExtension.collectOutputClassNames(kotlinFile)
        JetTestUtils.assertEqualsToFile(File(path.replace(".kt", ".expected.txt")), actualClasses.join("\n"))
    }
}
