/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.FirParser.Psi

open class ReflectionClassLoaderTest : CodegenTestCase() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = FirParser.LightTree

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
        loadFile("$prefix/differentClassLoaders.kt")

        doTest(
            createClassLoader(),
            createClassLoader()
        )
    }

    fun testClassLoaderWithNonTrivialEqualsAndHashCode() {
        // Check that class loaders do not participate as keys in hash maps (use identity hash maps instead)

        loadFile("$prefix/differentClassLoaders.kt")

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

        loadFile("$prefix/parentFirst.kt")

        class ChildClassLoader(parent: ClassLoader) : ClassLoader(parent)

        val parent = createClassLoader()

        doTest(
            parent,
            ChildClassLoader(parent)
        )
    }

    fun testKTypeEquality() {
        /*
         * Check that typeOf<List<Clz>>() when clz is loaded by different classloaders
         * differs in both its `equals` and its `classifier`.
         * It is important in the face of KType caching
         */
        loadFile("$prefix/kTypeEquality.kt")
        doTest(
            createClassLoader(),
            createClassLoader()
        )
    }
}
