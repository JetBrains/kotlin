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
import org.jetbrains.kotlin.metadata.ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION
import org.jetbrains.kotlin.metadata.ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

class JvmVersionRequirementTest : AbstractVersionRequirementTest() {
    override fun compileFiles(
        files: List<File>,
        outputDirectory: File,
        languageVersion: LanguageVersion,
        analysisFlags: Map<AnalysisFlag<*>, Any?>
    ) {
        LoadDescriptorUtil.compileKotlinToDirAndGetModule(
            listOf(File("compiler/testData/versionRequirement/${getTestName(true)}.kt")), outputDirectory,
            KotlinCoreEnvironment.createForTests(
                testRootDisposable,
                KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, outputDirectory).apply {
                    put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)
                    languageVersionSettings = LanguageVersionSettingsImpl(
                        languageVersion,
                        ApiVersion.createByLanguageVersion(languageVersion),
                        analysisFlags.toMap() + mapOf(AnalysisFlags.explicitApiVersion to true),
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

    fun testAllJvmDefault() {
        doTest(
            VersionRequirement.Version(1, 4, 0), DeprecationLevel.ERROR, null, COMPILER_VERSION, null,
            analysisFlags = mapOf(JvmAnalysisFlags.jvmDefaultMode to JvmDefaultMode.ALL_INCOMPATIBLE),
            fqNamesWithRequirements = listOf(
                "test.Base",
                "test.Derived",
                "test.BaseWithProperty",
                "test.DerivedWithProperty",
                "test.Empty",
                "test.EmptyWithNested",
                "test.WithAbstractDeclaration",
                "test.DerivedFromWithAbstractDeclaration"
            )
        )
    }

    fun testAllCompatibilityJvmDefault() {
        doTest(
            VersionRequirement.Version(1, 4, 0), DeprecationLevel.ERROR, null, COMPILER_VERSION, null,
            analysisFlags = mapOf(JvmAnalysisFlags.jvmDefaultMode to JvmDefaultMode.ALL_COMPATIBILITY),
            fqNamesWithRequirements = emptyList(),
            fqNamesWithoutRequirement = listOf(
                "test.Base",
                "test.Derived",
                "test.BaseWithProperty",
                "test.DerivedWithProperty",
                "test.Empty",
                "test.EmptyWithNested",
                "test.WithAbstractDeclaration",
                "test.DerivedFromWithAbstractDeclaration"
            )
        )
    }

    fun testInlineParameterNullCheck() {
        doTest(
            VersionRequirement.Version(1, 3, 50), DeprecationLevel.ERROR, null, COMPILER_VERSION, null,
            fqNamesWithRequirements = listOf(
                "test.doRun",
                "test.lambdaVarProperty",
                "test.extensionProperty"
            ),
            customLanguageVersion = LanguageVersion.KOTLIN_1_4
        )
    }

    fun testInlineClassReturnTypeMangled() {
        // Class members returning inline class values are mangled,
        // and have "since 1.4", "require 1.4.30" version requirement along with
        // "since 1.3" version requirement for inline class in signature
        doTest(
            VersionRequirement.Version(1, 4, 0), DeprecationLevel.ERROR, null, LANGUAGE_VERSION, null,
            fqNamesWithRequirements = listOf(
                "test.C.returnsInlineClassType",
                "test.C.propertyOfInlineClassType"
            ),
            customLanguageVersion = LanguageVersion.KOTLIN_1_4,
            shouldBeSingleRequirement = false
        )
        doTest(
            VersionRequirement.Version(1, 4, 30), DeprecationLevel.ERROR, null, COMPILER_VERSION, null,
            fqNamesWithRequirements = listOf(
                "test.C.propertyOfInlineClassType",
                "test.C.returnsInlineClassType",
            ),
            shouldBeSingleRequirement = false,
            customLanguageVersion = LanguageVersion.KOTLIN_1_4
        )
        // Top-level functions and properties returning inline class values are NOT mangled,
        // and have "since 1.3" version requirement for inline class in signature only.
        doTest(
            VersionRequirement.Version(1, 3, 0), DeprecationLevel.ERROR, null, LANGUAGE_VERSION, null,
            fqNamesWithRequirements = listOf(
                "test.propertyOfInlineClassType",
                "test.returnsInlineClassType",
            ),
            shouldBeSingleRequirement = false,
            customLanguageVersion = LanguageVersion.KOTLIN_1_4
        )
        // In Kotlin 1.3, all functions and properties returning inline class values are NOT mangled,
        // and have "since 1.3" version requirement for inline class in signature only.
        doTest(
            VersionRequirement.Version(1, 3, 0), DeprecationLevel.ERROR, null, LANGUAGE_VERSION, null,
            fqNamesWithRequirements = listOf(
                "test.propertyOfInlineClassType",
                "test.C.propertyOfInlineClassType",
                "test.C.returnsInlineClassTypeJvmName",
                "test.returnsInlineClassType",
                "test.C.returnsInlineClassType"
            ),
            shouldBeSingleRequirement = true,
            customLanguageVersion = LanguageVersion.KOTLIN_1_3
        )
    }

    fun testInlineClassesAndRelevantDeclarations1430() {
        doTest(
            VersionRequirement.Version(1, 4, 30), DeprecationLevel.ERROR, null, COMPILER_VERSION, null,
            fqNamesWithRequirements = listOf(
                "test.simpleFun",
                "test.aliasedFun",
            ),
            shouldBeSingleRequirement = false
        )
    }

    fun testContextReceivers() {
        doTest(
            VersionRequirement.Version(1, 6, 20), DeprecationLevel.ERROR, null, COMPILER_VERSION, null,
            fqNamesWithRequirements = listOf(
                "test.ClassWithCR",
                "test.ClassWithCR.memberPropWithCR",
                "test.ClassWithCR.memberFunWithCR",
                "test.topLevelFunWithCR",
                "test.topLevelPropWithCR"
            ),
            customLanguageVersion = LanguageVersion.KOTLIN_1_7
        )
    }
}
