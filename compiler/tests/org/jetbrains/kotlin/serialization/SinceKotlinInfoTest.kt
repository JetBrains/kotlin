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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.SinceKotlinInfo
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

class SinceKotlinInfoTest : TestCaseWithTmpdir() {
    fun doTest(
            fileName: String,
            expectedSinceKotlinInfoVersion: SinceKotlinInfo.Version,
            expectedLevel: DeprecationLevel,
            expectedMessage: String?,
            expectedErrorCode: Int?,
            vararg fqNames: String
    ) {
        LoadDescriptorUtil.compileKotlinToDirAndGetModule(
                listOf(File(fileName)), tmpdir,
                KotlinTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(testRootDisposable)
        )

        val (_, module) = JvmResolveUtil.analyze(
                KotlinCoreEnvironment.createForTests(
                        testRootDisposable,
                        KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, tmpdir),
                        EnvironmentConfigFiles.JVM_CONFIG_FILES
                )
        )

        fun check(descriptor: DeclarationDescriptor) {
            if (descriptor !is DeserializedMemberDescriptor) {
                throw AssertionError("Not a deserialized descriptor: $descriptor")
            }

            val sinceKotlinInfo = descriptor.sinceKotlinInfo ?: throw AssertionError("No SinceKotlinInfo for $descriptor")

            assertEquals(expectedSinceKotlinInfoVersion, sinceKotlinInfo.version)
            assertEquals(expectedLevel, sinceKotlinInfo.level)
            assertEquals(expectedMessage, sinceKotlinInfo.message)
            assertEquals(expectedErrorCode, sinceKotlinInfo.errorCode)
        }

        for (fqName in fqNames) {
            check(module.findUnambiguousDescriptorByFqName(fqName))
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

    fun testSuspendFun() {
        doTest("compiler/testData/sinceKotlinInfo/suspendFun.kt",
               SinceKotlinInfo.Version(1, 1), DeprecationLevel.ERROR, null, null,
               "test.topLevel",
               "test.Foo.member",
               "test.Foo.<init>",
               "test.async1",
               "test.async2",
               "test.async3",
               "test.async4",
               "test.asyncVal"
       )
    }
}
