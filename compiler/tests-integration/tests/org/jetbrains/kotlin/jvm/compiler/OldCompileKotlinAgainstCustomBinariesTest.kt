/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

class OldCompileKotlinAgainstCustomBinariesTest : AbstractCompileKotlinAgainstCustomBinariesTest() {
    override val languageVersion: LanguageVersion
        get() = LanguageVersion.KOTLIN_1_9

    private fun analyzeFileToPackageView(vararg extraClassPath: File): PackageViewDescriptor {
        val environment = createEnvironmentForDescriptorAnalysis(extraClassPath.toList())

        val ktFile = KotlinTestUtils.loadKtFile(environment.project, getTestDataFileWithExtension("kt"))
        val result = JvmResolveUtil.analyzeAndCheckForErrors(ktFile, environment)

        return result.moduleDescriptor.getPackage(LoadDescriptorUtil.TEST_PACKAGE_FQNAME).also {
            assertFalse("Failed to find package: " + LoadDescriptorUtil.TEST_PACKAGE_FQNAME, it.isEmpty())
        }
    }

    private fun createEnvironmentForDescriptorAnalysis(extraClassPath: List<File>): KotlinCoreEnvironment {
        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, *extraClassPath.toTypedArray())

        // Descriptors are not supposed to be used with LV >= 2.0, but also it's necessary to continue using old CoreJarFileSystem
        // as it affects in a subtle way on the presence of empty package in rendered descriptors.
        // The latter happens because CoreJarFileSystem seems to be ignoring empty directories, while FastJarFS doesn't
        // which doesn't look really crucial and a part of a contract of Jar FS.
        // Therefore, the LV is set to `languageVersion` which is 1.9 here.
        configuration.languageVersionSettings = LanguageVersionSettingsImpl(
            languageVersion,
            ApiVersion.createByLanguageVersion(languageVersion)
        )

        return KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    private fun analyzeAndGetAllDescriptors(vararg extraClassPath: File): Collection<DeclarationDescriptor> =
        DescriptorUtils.getAllDescriptors(analyzeFileToPackageView(*extraClassPath).memberScope)

    fun testDuplicateObjectInBinaryAndSources() {
        val allDescriptors = analyzeAndGetAllDescriptors(compileLibrary("library"))
        assertEquals(allDescriptors.toString(), 2, allDescriptors.size)
        for (descriptor in allDescriptors) {
            assertTrue("Wrong name: $descriptor", descriptor.name.asString() == "Lol")
            assertTrue("Should be an object: $descriptor", DescriptorUtils.isObject(descriptor))
        }
    }

    fun testBrokenJarWithNoClassForObject() {
        val brokenJar = copyJarFileWithoutEntry(compileLibrary("library"), "test/Lol.class")
        val allDescriptors = analyzeAndGetAllDescriptors(brokenJar)
        assertEmpty("No descriptors should be found: $allDescriptors", allDescriptors)
    }
}
