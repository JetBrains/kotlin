/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase.getTestName
import org.junit.jupiter.api.Test
import java.io.File

class JvmVersionRequirementTest : TestCaseWithTmpdir() {
    private fun doTest(
        expectedVersionRequirement: VersionRequirement.Version,
        expectedLevel: DeprecationLevel,
        expectedMessage: String?,
        expectedVersionKind: ProtoBuf.VersionRequirement.VersionKind,
        expectedErrorCode: Int?,
        fqNamesWithRequirements: List<String>,
    ) {
        compileFiles(tmpdir)
        val module = loadModule()

        for (fqName in fqNamesWithRequirements) {
            val descriptor = module.findUnambiguousDescriptorByFqName(fqName)

            val requirements = extractRequirement(descriptor)
            if (requirements.isEmpty()) throw AssertionError("No VersionRequirement for $descriptor")

            val requiredVersion = requirements.firstOrNull {
                expectedVersionRequirement == it.version &&
                        expectedLevel == it.level &&
                        expectedMessage == it.message &&
                        expectedVersionKind == it.kind &&
                        expectedErrorCode == it.errorCode
            }
            if (requiredVersion == null)
                throw AssertionError(
                    "Version requirement not found, expected:\n" +
                            "versionRequirement=" + expectedVersionRequirement +
                            "; level=" + expectedLevel +
                            "; message=" + expectedMessage +
                            "; versionKind=" + expectedVersionKind +
                            "; errorCode=" + expectedErrorCode +
                            "\nActual requirements:\n" +
                            requirements.joinToString(separator = "\n") { it.toDebugString() }
                )
        }
    }

    private fun VersionRequirement.toDebugString(): String =
        "versionRequirement=$version; level=$level; message=$message; versionKind=$kind; errorCode=$errorCode"

    private fun extractRequirement(descriptor: DeclarationDescriptor): List<VersionRequirement> {
        return when (descriptor) {
            is DeserializedMemberDescriptor -> descriptor.versionRequirements
            is DeserializedClassDescriptor -> descriptor.versionRequirements
            else -> throw AssertionError("Unknown descriptor: $descriptor")
        }
    }

    private fun ModuleDescriptor.findUnambiguousDescriptorByFqName(fqName: String): DeclarationDescriptor {
        val names = fqName.split('.')
        var descriptor: DeclarationDescriptor = getPackage(FqName(names.first()))
        for (name in names.drop(1)) {
            val descriptors = when (name) {
                "<init>" -> (descriptor as ClassDescriptor).constructors
                else -> {
                    val scope = when (descriptor) {
                        is PackageViewDescriptor -> descriptor.memberScope
                        is ClassDescriptor -> descriptor.unsubstitutedMemberScope
                        else -> error("Unsupported: $descriptor")
                    }
                    scope.getDescriptorsFiltered(nameFilter = { it.asString() == name })
                }
            }
            if (descriptors.isEmpty()) throw AssertionError("Descriptor not found: $name in $descriptor")
            descriptor = descriptors.singleOrNull() ?: throw AssertionError("Not a unambiguous descriptor: $name in $descriptor")
        }
        return descriptor
    }

    private fun compileFiles(outputDirectory: File) {
        LoadDescriptorUtil.compileKotlinToDirAndGetModule(
            listOf(
                ForTestCompileRuntime.transformTestDataPath(
                    "compiler/testData/versionRequirement/${getTestName(testInfo.testMethod.get().name, true)}.kt"
                )
            ),
            outputDirectory,
            @OptIn(CoreEnvironmentDeprecation::class)
            KotlinCoreEnvironment.createForTests(
                testRootDisposable,
                KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, outputDirectory),
                EnvironmentConfigFiles.JVM_CONFIG_FILES,
            )
        )
    }

    private fun loadModule(): ModuleDescriptor {
        @Suppress("DEPRECATION_ERROR")
        return JvmResolveUtil.analyze(
            @OptIn(CoreEnvironmentDeprecation::class)
            KotlinCoreEnvironment.createForTests(
                testRootDisposable,
                KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, tmpdir),
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )
        ).moduleDescriptor
    }

    @Test
    fun testLanguageVersionViaAnnotation() {
        doTest(
            VersionRequirement.Version(1, 1), DeprecationLevel.WARNING, "message",
            ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION, 42,
            fqNamesWithRequirements = listOf(
                "test.Klass",
                "test.Konstructor.<init>",
                "test.Typealias",
                "test.function",
                "test.property"
            )
        )
    }

    @Test
    fun testApiVersionViaAnnotation() {
        doTest(
            VersionRequirement.Version(1, 1), DeprecationLevel.WARNING, "message", ProtoBuf.VersionRequirement.VersionKind.API_VERSION, 42,
            fqNamesWithRequirements = listOf(
                "test.Klass",
                "test.Konstructor.<init>",
                "test.Typealias",
                "test.function",
                "test.property"
            )
        )
    }

    @Test
    fun testCompilerVersionViaAnnotation() {
        doTest(
            VersionRequirement.Version(1, 1), DeprecationLevel.WARNING, "message",
            ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION, 42,
            fqNamesWithRequirements = listOf(
                "test.Klass",
                "test.Konstructor.<init>",
                "test.Typealias",
                "test.function",
                "test.property"
            )
        )
    }

    @Test
    fun testPatchVersion() {
        doTest(
            VersionRequirement.Version(1, 1, 50), DeprecationLevel.HIDDEN, null,
            ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION, null,
            fqNamesWithRequirements = listOf("test.Klass")
        )
    }

    @Test
    fun testNestedClassMembers() {
        doTest(
            VersionRequirement.Version(1, 3), DeprecationLevel.ERROR, null, ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION, null,
            fqNamesWithRequirements = listOf(
                "test.Outer.Inner.Deep",
                "test.Outer.Inner.Deep.<init>",
                "test.Outer.Inner.Deep.f",
                "test.Outer.Inner.Deep.x",
                "test.Outer.Nested.g",
                "test.Outer.Companion"
            )
        )
    }
}
