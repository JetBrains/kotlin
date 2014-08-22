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

package org.jetbrains.jet.lang.resolve.android

import com.intellij.testFramework.UsefulTestCase
import java.io.File
import java.io.IOException
import java.util.Scanner
import java.io.FileWriter
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.jet.config.CompilerConfiguration
import org.jetbrains.jet.JetTestUtils
import org.jetbrains.jet.ConfigurationKind
import org.jetbrains.jet.TestJdkKind
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys
import org.junit.Assert
import kotlin.test.fail

public abstract class AbstractAndroidXml2KConversionTest : UsefulTestCase() {

    public fun doTest(path: String) {
        val jetCoreEnvironment = getEnvironment(path)
        val parser = CliAndroidUIXmlProcessor(jetCoreEnvironment.getProject(), path + "/layout", path + "AndroidManifest.xml")

        val actual = parser.parseToString()

        JetTestUtils.assertEqualsToFile(File(path + "/layout.kt"), actual!!)
    }

    public fun doNoManifestTest(path: String) {
        try {
            doTest(path)
            fail("NoAndroidManifestFound not thrown")
        }
        catch (e: AndroidUIXmlProcessor.NoAndroidManifestFound) {
        }
    }

    private fun getEnvironment(testPath: String): JetCoreEnvironment {
        val configuration = JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK)
//        configuration.put<String>(JVMConfigurationKeys.ANDROID_RES_PATH, testPath + "/layout")
//        configuration.put<String>(JVMConfigurationKeys.ANDROID_MANIFEST, testPath + "/AndroidManifest.xml")
        return JetCoreEnvironment.createForTests(getTestRootDisposable()!!, configuration)
    }
}
