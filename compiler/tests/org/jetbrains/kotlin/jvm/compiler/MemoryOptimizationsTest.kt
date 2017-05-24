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

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.lowerIfFlexible
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.upperIfFlexible
import org.junit.Test

class MemoryOptimizationsTest : KtUsefulTestCase() {
    @Test
    fun testBasicFlexibleTypeCase() {
        val moduleDescriptor = JvmResolveUtil.analyze(
                KotlinTestUtils.createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(myTestRootDisposable, ConfigurationKind.ALL, TestJdkKind.FULL_JDK)
        ).moduleDescriptor

        val appendableClass =
                moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("java.lang.Appendable")))!!

        val append = appendableClass
                .unsubstitutedMemberScope
                .findFirstFunction("append") { it.valueParameters.singleOrNull()?.type?.let(KotlinBuiltIns::isChar) == false }

        val parameterType = append.valueParameters.single().type

        assertTrue(parameterType is FlexibleType)
        val upperBound = parameterType.upperIfFlexible()

        assertTrue(upperBound.javaClass.simpleName == "NullableSimpleType")
        // NullableSimpleType should store and return the same instance as lower bound of flexible type
        assertTrue(parameterType.lowerIfFlexible() === upperBound.makeNullableAsSpecified(false))
    }



    @Test
    fun testSubstitutorDoNotRecreateUnchangedDescriptor() {
        val text =
                """
                |package test
                |interface A<T> : java.lang.Appendable {
                |   fun foo(x: T)
                |}
                """.trimMargin()

        val environment =
                KotlinTestUtils
                        .createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(
                                myTestRootDisposable, ConfigurationKind.ALL, TestJdkKind.FULL_JDK
                        )
        val moduleDescriptor =
                JvmResolveUtil.analyze(
                        KotlinTestUtils.createFile("main.kt", text, environment.project),
                        environment
                ).moduleDescriptor

        val aClass =
                moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("test.A")))!!

        val memberScope =
                aClass.getMemberScope(
                        TypeConstructorSubstitution.create(
                                aClass.typeConstructor, listOf(moduleDescriptor.builtIns.stringType.asTypeProjection())
                        )
                )

        val append =
                memberScope.findFirstFunction("append") {
                    it.valueParameters.singleOrNull()?.type?.let(KotlinBuiltIns::isChar) == false
                }

        assertTrue(append.original === append)

        val foo = memberScope.findSingleFunction(Name.identifier("foo"))

        assertTrue(foo.original !== foo)
    }
}
