/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.test.ConfigurationKind

class ReflectionClassLoaderTest : CodegenTestCase() {
    override fun getPrefix() = "reflection/classLoaders"

    override fun setUp() {
        super.setUp()
        configurationKind = ConfigurationKind.ALL
        createEnvironmentWithMockJdkAndIdeaAnnotations(configurationKind)
    }

    private fun Class<*>.methodByName(name: String) = declaredMethods.single { it.name == name }

    fun doTest(cl1: ClassLoader, cl2: ClassLoader) {
        val t1 = cl1.loadClass("test.Test")
        val t2 = cl2.loadClass("test.Test")

        fun Class<*>.getKClass() = methodByName("kClass")(newInstance())

        t1.methodByName("doTest")(t1.newInstance(), t1.getKClass(), t2.getKClass())
    }

    fun testSimpleDifferentClassLoaders() {
        loadFile(prefix + "/differentClassLoaders.kt")

        doTest(
                createClassLoader(),
                createClassLoader()
        )
    }

    fun testClassLoaderWithNonTrivialEqualsAndHashCode() {
        // Check that class loaders do not participate as keys in hash maps (use identity hash maps instead)

        loadFile(prefix + "/differentClassLoaders.kt")

        class BrokenEqualsClassLoader(parent: ClassLoader) : ClassLoader(parent) {
            override fun equals(other: Any?) = true
            override fun hashCode() = 0
        }

        doTest(
                BrokenEqualsClassLoader(createClassLoader()),
                BrokenEqualsClassLoader(createClassLoader())
        )
    }

    fun testParentFirst() {
        // Check that for a child class loader, a class reference would be the same as for his parent

        loadFile(prefix + "/parentFirst.kt")

        class ChildClassLoader(parent: ClassLoader) : ClassLoader(parent)

        val parent = createClassLoader()

        doTest(
                parent,
                ChildClassLoader(parent)
        )
    }
}
