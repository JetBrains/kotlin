/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.config.AnalysisFlag
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
        shouldBeSingleRequirement: Boolean = true
    ) {
        compileFiles(
            listOf(File("compiler/testData/versionRequirement/${getTestName(true)}.kt")),
            tmpdir, customLanguageVersion, analysisFlags
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
        analysisFlags: Map<AnalysisFlag<*>, Any?>
    )

    protected abstract fun loadModule(directory: File): ModuleDescriptor

    fun testSuspendFun() {
        doTest(
            VersionRequirement.Version(1, 3), DeprecationLevel.ERROR, null, ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION, null,
            customLanguageVersion = LanguageVersion.KOTLIN_1_3,
            fqNamesWithRequirements = listOf(
                "test.topLevel",
                "test.Foo.member",
                "test.Foo.<init>",
                "test.async1",
                "test.async2",
                "test.async3",
                "test.async4",
                "test.asyncVal"
            )
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
                "test.Outer.Inner.Deep.s",
                "test.Outer.Nested.g",
                "test.Outer.Companion"
            )
        )
    }

    fun testInlineClassesAndRelevantDeclarations13() {
        doTest(
            VersionRequirement.Version(1, 3), DeprecationLevel.ERROR, null, ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION, null,
            fqNamesWithRequirements = listOf(
                "test.IC",
                "test.Ctor.<init>",
                "test.Foo",
                "test.Bar",
                "test.simpleProp"
            )
        )
    }

    fun testInlineClassesAndRelevantDeclarations1430() {
        doTest(
            VersionRequirement.Version(1, 4, 30), DeprecationLevel.ERROR, null, ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION, null,
            fqNamesWithRequirements = listOf(
                "test.simpleFun",
                "test.aliasedFun",
                "test.result",
            )
        )
    }
}
