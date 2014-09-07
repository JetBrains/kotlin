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
import org.jetbrains.jet.plugin.PluginTestCaseBase
import org.jetbrains.jet.test.TestMetadata
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase
import com.intellij.ide.startup.impl.StartupManagerImpl
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import org.jetbrains.jet.JetTestCaseBuilder
import com.intellij.openapi.startup.StartupManager
import com.android.SdkConstants
import com.intellij.openapi.application.PathManager

public abstract class AbstractCrossParserTest : KotlinAndroidTestCase() {
    public fun doTest(path: String) {
        val project = myFixture!!.getProject()
        project.putUserData(TestConst.TESTDATA_PATH, path)
        myFixture!!.copyDirectoryToProject(getResDir()!!, "res")
        val cliParser = CliAndroidUIXmlProcessor(project, path + getResDir() + "/layout/", path + "../AndroidManifest.xml")
        val ideParser = IDEAndroidUIXmlProcessor(project)

        val cliResult = cliParser.parseToPsi(project)!!.getText()
        val ideResult = ideParser.parseToPsi(project)!!.getText()

        assertEquals(cliResult, ideResult)
    }
    override fun setUp() {
        System.setProperty(KotlinAndroidTestCaseBase.SDK_PATH_PROPERTY, PathManager.getHomePath() + "/androidSDK/")
        System.setProperty(KotlinAndroidTestCaseBase.PLATFORM_DIR_PROPERTY, "android-17")
        super.setUp()
    }

    private fun getEnvironment(testPath: String): JetCoreEnvironment {
        val configuration = JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK)
                configuration.put<String>(JVMConfigurationKeys.ANDROID_RES_PATH, testPath + "/layout")
                configuration.put<String>(JVMConfigurationKeys.ANDROID_MANIFEST, testPath + "/AndroidManifest.xml")
        return JetCoreEnvironment.createForTests(getTestRootDisposable()!!, configuration)
    }

    override fun getTestDataPath(): String? {
        return PluginTestCaseBase.getTestDataPathBase() + "/android/crossParser/" + getTestName(true) + "/"
    }

    override fun createManifest() {
        myFixture!!.copyFileToProject("idea/testData/android/AndroidManifest.xml", SdkConstants.FN_ANDROID_MANIFEST_XML)
    }

    override fun requireRecentSdk() = true
}