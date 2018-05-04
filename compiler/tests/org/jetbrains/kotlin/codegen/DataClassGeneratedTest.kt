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

package org.jetbrains.kotlin.codegen;

import org.jetbrains.kotlin.test.ConfigurationKind;
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataClassGeneratedTest : CodegenTestCase() {

    override protected fun setUp() {
        super.setUp()
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL)
    }

    @Test
    fun testNoOverrides() {
        loadText("data class User(val name: String, var age: Int)")
        val clazz = generateClass("User")
        val methods = hashSetOf("toString", "hashCode", "equals", "component1", "component2", "copy")

        hasGeneratedAnnotationForMethods(clazz, methods)
    }

    @Test
    fun testOverrideToString() {
        loadText(
                "data class User(val name: String, var age: Int) {" +
                        "override fun toString(): String = \"TEST\"" +
                        "}")
        val clazz = generateClass("User")
        val methods = hashSetOf("hashCode", "equals", "component1", "component2", "copy")

        hasGeneratedAnnotationForMethods(clazz, methods)
    }

    @Test
    fun testOverrideHashCode() {
        loadText(
                "data class User(val name: String, var age: Int) {" +
                        "override fun hashCode(): Int = 123" +
                        "}")
        val clazz = generateClass("User")
        val methods = hashSetOf("toString", "equals", "component1", "component2", "copy")

        hasGeneratedAnnotationForMethods(clazz, methods)
    }

    @Test
    fun testOverrideEquals() {
        loadText(
                "data class User(val name: String, var age: Int) {" +
                        "override fun equals(other: Any?): Boolean = true" +
                        "}")
        val clazz = generateClass("User")
        val methods = hashSetOf("toString", "hashCode", "component1", "component2", "copy")

        hasGeneratedAnnotationForMethods(clazz, methods)
    }

    private fun hasGeneratedAnnotationForMethods(clazz: Class<*>, methodNames: Set<String>) {
        val annotation = kotlin.Generated::class.java
        for (method in clazz.declaredMethods) {
            method.run {
                if (methodNames.contains(name)) {
                    assertTrue(
                            "Method $name() annotated with ${annotation.canonicalName}",
                            isAnnotationPresent(annotation))
                } else {
                    assertFalse(
                            "Method $name() is not annotated with ${annotation.canonicalName}",
                            isAnnotationPresent(annotation))
                }
            }
        }
    }
}
