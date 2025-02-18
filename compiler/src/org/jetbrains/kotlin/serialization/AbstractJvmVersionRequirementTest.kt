/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.FrontendBackendConfiguration
import java.io.File

abstract class AbstractJvmVersionRequirementTest : AbstractVersionRequirementTest(), FrontendBackendConfiguration {
    override fun compileFiles(
        files: List<File>,
        outputDirectory: File,
        languageVersion: LanguageVersion,
        analysisFlags: Map<AnalysisFlag<*>, Any?>,
        specificFeatures: Map<LanguageFeature, LanguageFeature.State>,
    ) {
        LoadDescriptorUtil.compileKotlinToDirAndGetModule(
            listOf(File("compiler/testData/versionRequirement/${getTestName(true)}.kt")), outputDirectory,
            KotlinCoreEnvironment.createForTests(
                testRootDisposable,
                KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, outputDirectory).apply {
                    languageVersionSettings = LanguageVersionSettingsImpl(
                        languageVersion,
                        ApiVersion.createByLanguageVersion(languageVersion),
                        analysisFlags.toMap() + mapOf(AnalysisFlags.explicitApiVersion to true),
                        specificFeatures
                    )
                }.also {
                    configureIrFir(it)
                },
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )
        )
    }

    override fun loadModule(directory: File): ModuleDescriptor = JvmResolveUtil.analyze(
        KotlinCoreEnvironment.createForTests(
            testRootDisposable,
            KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, tmpdir).also {
                configureIrFir(it)
            },
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    ).moduleDescriptor
}
