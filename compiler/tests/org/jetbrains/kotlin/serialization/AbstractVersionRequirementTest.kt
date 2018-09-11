/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization

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
        fqNames: List<String>
    ) {
        compileFiles(listOf(File("compiler/testData/versionRequirement/${getTestName(true)}.kt")), tmpdir, customLanguageVersion)
        val module = loadModule(tmpdir)

        for (fqName in fqNames) {
            val descriptor = module.findUnambiguousDescriptorByFqName(fqName)

            val requirement = when (descriptor) {
                is DeserializedMemberDescriptor -> descriptor.versionRequirements.single()
                is DeserializedClassDescriptor -> descriptor.versionRequirements.single()
                else -> throw AssertionError("Unknown descriptor: $descriptor")
            } ?: throw AssertionError("No VersionRequirement for $descriptor")

            assertEquals("Incorrect version for $fqName", expectedVersionRequirement, requirement.version)
            assertEquals("Incorrect level for $fqName", expectedLevel, requirement.level)
            assertEquals("Incorrect message for $fqName", expectedMessage, requirement.message)
            assertEquals("Incorrect versionKind for $fqName", expectedVersionKind, requirement.kind)
            assertEquals("Incorrect errorCode for $fqName", expectedErrorCode, requirement.errorCode)
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

    protected abstract fun compileFiles(files: List<File>, outputDirectory: File, languageVersion: LanguageVersion)

    protected abstract fun loadModule(directory: File): ModuleDescriptor

    fun testSuspendFun() {
        doTest(
            VersionRequirement.Version(1, 1), DeprecationLevel.ERROR, null, ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION, null,
            customLanguageVersion = LanguageVersion.KOTLIN_1_2,
            fqNames = listOf(
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

        doTest(
            VersionRequirement.Version(1, 3), DeprecationLevel.ERROR, null, ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION, null,
            customLanguageVersion = LanguageVersion.KOTLIN_1_3,
            fqNames = listOf(
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
            fqNames = listOf(
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
            fqNames = listOf(
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
            fqNames = listOf(
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
            fqNames = listOf("test.Klass")
        )
    }

    fun testNestedClassMembers() {
        doTest(
            VersionRequirement.Version(1, 1), DeprecationLevel.ERROR, null, ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION, null,
            customLanguageVersion = LanguageVersion.KOTLIN_1_2,
            fqNames = listOf(
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

    fun testInlineClassesAndRelevantDeclarations() {
        doTest(
            VersionRequirement.Version(1, 3), DeprecationLevel.ERROR, null, ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION, null,
            customLanguageVersion = LanguageVersion.KOTLIN_1_2,
            fqNames = listOf(
                "test.IC",
                "test.Ctor.<init>",
                "test.simpleFun",
                "test.aliasedFun",
                "test.simpleProp",
                "test.result",
                "test.Foo",
                "test.Bar"
            )
        )
    }
}
