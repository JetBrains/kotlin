/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLClassLoader

class ReflectionClassLoaderTest : AbstractKotlinCompilerIntegrationTest() {
    override val testDataPath: String
        get() = tmpdir.absolutePath

    private fun compile(source: String): File {
        File(testDataDirectory, "src/test.kt").apply { parentFile.mkdirs() }.writeText(source)
        return compileLibrary(
            "src",
            testDataDirectory.resolve("out"),
            extraClassPath = listOf(ForTestCompileRuntime.kotlinTestJarForTests()),
        )
    }

    private fun createClassLoader(output: File): ClassLoader =
        URLClassLoader(arrayOf(output.toURI().toURL()), ForTestCompileRuntime.runtimeAndReflectJarClassLoader())

    private fun Class<*>.methodByName(name: String) = declaredMethods.single { it.name == name }

    private fun doTest(cl1: ClassLoader, cl2: ClassLoader) {
        val t1 = cl1.loadClass("test.Test")
        val t2 = cl2.loadClass("test.Test")

        fun Class<*>.getKClass() = methodByName("kClass")(getDeclaredConstructor().newInstance())

        t1.methodByName("doTest")(t1.getDeclaredConstructor().newInstance(), t1.getKClass(), t2.getKClass())
    }

    @Test
    fun testSimpleDifferentClassLoaders() {
        val output = compile(DIFFERENT_CLASS_LOADERS)

        doTest(
            createClassLoader(output),
            createClassLoader(output),
        )
    }

    @Test
    fun testClassLoaderWithNonTrivialEqualsAndHashCode() {
        // Check that class loaders do not participate as keys in hash maps (use identity hash maps instead)

        val output = compile(DIFFERENT_CLASS_LOADERS)

        class BrokenEqualsClassLoader(parent: ClassLoader) : ClassLoader(parent) {
            override fun equals(other: Any?) = true
            override fun hashCode() = 0
        }

        doTest(
            BrokenEqualsClassLoader(createClassLoader(output)),
            BrokenEqualsClassLoader(createClassLoader(output)),
        )
    }

    @Test
    fun testParentFirst() {
        // Check that for a child class loader, a class reference would be the same as for his parent

        val output = compile(
            """
                package test
    
                import kotlin.reflect.KClass
                import kotlin.test.*
    
                class K
    
                class Test {
                    fun kClass(): Any = K::class
    
                    fun doTest(k1: KClass<*>, k2: KClass<*>) {
                        // KClass instances should be equal for classes loaded with the child and the parent
                        assertEquals(k1, k2)
                    }
                }
            """.trimIndent()
        )

        class ChildClassLoader(parent: ClassLoader) : ClassLoader(parent)

        val parent = createClassLoader(output)

        doTest(
            parent,
            ChildClassLoader(parent)
        )
    }

    @Test
    fun testKTypeEquality() {
        // Check that typeOf<List<Clz>>() when clz is loaded by different classloaders
        // differs in both its `equals` and its `classifier`.
        // It is important in the face of KType caching

        val output = compile(
            """
                package test
    
                import kotlin.reflect.*
                import kotlin.reflect.full.*
                import kotlin.test.*
    
                class K {
                    fun getType() = typeOf<List<K>>()
                }
    
                class Test {
                    fun kClass(): Any = K::class
    
                    fun KClass<*>.invokeGetType() =
                        java.declaredMethods.single { it.name == "getType" }.invoke(java.getDeclaredConstructor().newInstance()) as KType
    
                    fun doTest(k1: KClass<*>, k2: KClass<*>) {
                        assertNotEquals(k1, k2)
    
                        val type1 = k1.invokeGetType()
                        val type2 = k2.invokeGetType()
                        assertNotEquals((type1.arguments[0].type)?.classifier, (type2.arguments[0].type)?.classifier)
                        assertNotEquals(type1, type2)
                    }
                }
            """.trimIndent()
        )

        doTest(
            createClassLoader(output),
            createClassLoader(output),
        )
    }

    companion object {
        private val DIFFERENT_CLASS_LOADERS = $$"""
            package test

            import kotlin.reflect.KClass
            import kotlin.reflect.full.*
            import kotlin.test.*

            class K(val p: String)

            class Test {
                fun kClass(): Any = K::class

                fun doTest(k1: KClass<*>, k2: KClass<*>) {
                    // KClass instances for classes loaded with different class loaders should have the same string representation,
                    // but should not be equal
                    assertEquals("$k1", "$k2")
                    assertNotEquals(k1, k2)

                    // The same for properties of these classes
                    val p1 = k1.memberProperties.first()
                    val p2 = k2.memberProperties.first()
                    assertEquals("$p1", "$p2")
                    assertNotEquals(p1, p2)
                }
            }
        """.trimIndent()
    }
}
