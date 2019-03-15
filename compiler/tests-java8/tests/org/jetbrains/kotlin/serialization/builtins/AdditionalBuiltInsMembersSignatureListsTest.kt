/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.serialization.builtins

import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSettings
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.resolveClassByFqName
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.TestJdkKind

class AdditionalBuiltInsMembersSignatureListsTest : KotlinTestWithEnvironment() {
    override fun createEnvironment(): KotlinCoreEnvironment {
        return createEnvironmentWithJdk(ConfigurationKind.JDK_ONLY, TestJdkKind.FULL_JDK)
    }

    fun testAllListedSignaturesExistInJdk() {
        val module = JvmResolveUtil.analyze(environment).moduleDescriptor as ModuleDescriptorImpl

        val blackList =
                JvmBuiltInsSettings.BLACK_LIST_METHOD_SIGNATURES +
                JvmBuiltInsSettings.MUTABLE_METHOD_SIGNATURES +
                JvmBuiltInsSettings.BLACK_LIST_CONSTRUCTOR_SIGNATURES +
                JvmBuiltInsSettings.WHITE_LIST_METHOD_SIGNATURES +
                JvmBuiltInsSettings.WHITE_LIST_CONSTRUCTOR_SIGNATURES

        val groupedByInternalName = blackList.groupBy({ it.split(".")[0] }) { it.split(".")[1] }

        groupedByInternalName.entries.forEach {
            it ->
            val (internalName, jvmDescriptors) = it
            val classDescriptor =
                    module.resolveClassByFqName(
                            JvmClassName.byInternalName(internalName).fqNameForClassNameWithoutDollars, NoLookupLocation.FROM_TEST
                    )!!

            val scope = classDescriptor.unsubstitutedMemberScope

            val lateJdkSignatures = LATE_JDK_SIGNATURES[internalName] ?: emptySet()

            jvmDescriptors.forEach {
                jvmDescriptor ->

                if (jvmDescriptor in lateJdkSignatures) return@forEach

                val stringName = jvmDescriptor.split("(")[0]
                val functions =
                        if (stringName == "<init>")
                            classDescriptor.constructors
                        else
                            scope.getContributedFunctions(Name.identifier(stringName), NoLookupLocation.FROM_TEST)

                functions.singleOrNull {
                    it.isEffectivelyPublicApi && it.computeJvmDescriptor() == jvmDescriptor
                } ?: fail("Expected single function with signature $jvmDescriptor in $internalName")
            }
        }
    }

    private val LATE_JDK_SIGNATURES = mapOf(
        "java/lang/String" to setOf("isBlank()Z", "lines()Ljava/util/stream/Stream;", "repeat(I)Ljava/lang/String;")
    )
}
