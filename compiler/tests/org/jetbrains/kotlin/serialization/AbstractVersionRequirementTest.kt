/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import java.io.File

abstract class AbstractVersionRequirementTest : TestCaseWithTmpdir() {
    fun doTest(
        expectedVersionRequirement: VersionRequirement.Version,
        expectedLevel: DeprecationLevel,
        expectedMessage: String?,
        expectedVersionKind: ProtoBuf.VersionRequirement.VersionKind,
        expectedErrorCode: Int?,
        customLanguageVersion: LanguageVersion = LanguageVersionSettingsImpl.DEFAULT.languageVersion,
        analysisFlags: Map<AnalysisFlag<*>, Any?> = emptyMap(),
        fqNamesWithRequirements: List<String>,
        fqNamesWithoutRequirement: List<String> = emptyList(),
        shouldBeSingleRequirement: Boolean = true,
        specificFeatures: Map<LanguageFeature, LanguageFeature.State> = emptyMap()
    ) {
        compileFiles(
            listOf(File("compiler/testData/versionRequirement/${getTestName(true)}.kt")),
            tmpdir, customLanguageVersion, analysisFlags, specificFeatures
        )
        val module = loadModule(tmpdir)

        for (fqName in fqNamesWithRequirements) {
            val descriptor = module.findUnambiguousDescriptorByFqName(fqName)

            val requirements = extractRequirement(descriptor)
            if (requirements.isEmpty()) throw AssertionError("No VersionRequirement for $descriptor")

            if (shouldBeSingleRequirement && requirements.size > 1) {
                throw AssertionError(
                    "Single VersionRequirement expected, got ${requirements.size}:\n" +
                            requirements.joinToString(separator = "\n") { it.toDebugString() }
                )
            }

            requirements.firstOrNull {
                expectedVersionRequirement == it.version &&
                        expectedLevel == it.level &&
                        expectedMessage == it.message &&
                        expectedVersionKind == it.kind &&
                        expectedErrorCode == it.errorCode
            }
                ?: throw AssertionError(
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

        for (fqName in fqNamesWithoutRequirement) {
            val descriptor = module.findUnambiguousDescriptorByFqName(fqName)

            val requirement = extractRequirement(descriptor)
            assertTrue("Expecting absence of any requirements for $fqName, but `$requirement`", requirement.isEmpty())
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

    protected abstract fun compileFiles(
        files: List<File>,
        outputDirectory: File,
        languageVersion: LanguageVersion,
        analysisFlags: Map<AnalysisFlag<*>, Any?>,
        specificFeatures: Map<LanguageFeature, LanguageFeature.State>
    )

    protected abstract fun loadModule(directory: File): ModuleDescriptor

    fun testDefinitelyNotNull() {
        doTest(
            VersionRequirement.Version(1, 7), DeprecationLevel.ERROR, null, ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION, null,
            customLanguageVersion = LanguageVersion.KOTLIN_1_7,
            fqNamesWithRequirements = listOf(
                "test.A.foo",
                "test.A.w",
                "test.B.<init>",
                "test.bar1",
                "test.bar2",
                "test.nn",
                "test.Outer.R1",
                "test.Outer.R2",
                "test.Alias",
            ),
            fqNamesWithoutRequirement = listOf(
                "test.Outer",
                "test.Outer.W",
            ),
        )
    }

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

    fun testPatchVersion() {
        doTest(
            VersionRequirement.Version(1, 1, 50), DeprecationLevel.HIDDEN, null,
            ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION, null,
            fqNamesWithRequirements = listOf("test.Klass")
        )
    }

    fun testNestedClassMembers() {
        doTest(
            VersionRequirement.Version(1, 3), DeprecationLevel.ERROR, null, ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION, null,
            customLanguageVersion = LanguageVersion.KOTLIN_1_3,
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
