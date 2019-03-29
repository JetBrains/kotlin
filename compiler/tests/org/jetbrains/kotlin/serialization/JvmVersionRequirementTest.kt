/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil
import org.jetbrains.kotlin.metadata.ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.resolve.JvmTarget
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

class JvmVersionRequirementTest : AbstractVersionRequirementTest() {
    override fun compileFiles(files: List<File>, outputDirectory: File, languageVersion: LanguageVersion) {
        LoadDescriptorUtil.compileKotlinToDirAndGetModule(
            listOf(File("compiler/testData/versionRequirement/${getTestName(true)}.kt")), outputDirectory,
            KotlinCoreEnvironment.createForTests(
                testRootDisposable,
                KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, outputDirectory).apply {
                    put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)
                    languageVersionSettings = LanguageVersionSettingsImpl(
                        languageVersion,
                        ApiVersion.createByLanguageVersion(languageVersion),
                        mapOf(JvmAnalysisFlags.jvmDefaultMode to JvmDefaultMode.ENABLE),
                        emptyMap()
                    )
                },
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )
        )
    }

    override fun loadModule(directory: File): ModuleDescriptor = JvmResolveUtil.analyze(
        KotlinCoreEnvironment.createForTests(
            testRootDisposable,
            KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, tmpdir),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    ).moduleDescriptor

    fun testJvmDefault() {
        doTest(
            VersionRequirement.Version(1, 2, 40), DeprecationLevel.ERROR, null, COMPILER_VERSION, null,
            fqNames = listOf(
                "test.Base",
                "test.Derived"
            )
        )
    }

    fun testJvmFieldInInterfaceCompanion() {
        doTest(
            VersionRequirement.Version(1, 2, 70), DeprecationLevel.ERROR, null, COMPILER_VERSION, null,
            fqNames = listOf("test.Base.Companion.foo")
        )
    }
}
