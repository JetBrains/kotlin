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

package org.jetbrains.jet.android

import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.jet.lang.resolve.android.CliAndroidUIXmlProcessor
import org.jetbrains.jet.JetTestUtils
import org.jetbrains.jet.ConfigurationKind
import org.jetbrains.jet.TestJdkKind
import org.jetbrains.jet.plugin.android.IDEAndroidUIXmlProcessor
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys
import junit.framework.TestCase
import kotlin.test.assertEquals
import org.jetbrains.jet.plugin.android.TestConst
import com.intellij.testFramework.LightCodeInsightTestCase
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase

public abstract class AbstractCrossParserTest : LightCodeInsightFixtureTestCase() {
    public fun doTest(path: String) {
        val jetCoreEnvironment = getEnvironment(path)
        val project = jetCoreEnvironment.getProject()
        project.putUserData(TestConst.TESTDATA_PATH, path)
        val cliParser = CliAndroidUIXmlProcessor(jetCoreEnvironment.getProject(), path + "/layout", path + "AndroidManifest.xml")
        val ideParser = IDEAndroidUIXmlProcessor(jetCoreEnvironment.getProject())

        val cliResult = cliParser.parseToPsi(project)!!.getText()
        val ideResult = ideParser.parseToPsi(project)!!.getText()

        assertEquals(cliResult, ideResult)
    }

    private fun getEnvironment(testPath: String): JetCoreEnvironment {
        val configuration = JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK)
                configuration.put<String>(JVMConfigurationKeys.ANDROID_RES_PATH, testPath + "/layout")
                configuration.put<String>(JVMConfigurationKeys.ANDROID_MANIFEST, testPath + "/AndroidManifest.xml")
        return JetCoreEnvironment.createForTests(getTestRootDisposable()!!, configuration)
    }
}