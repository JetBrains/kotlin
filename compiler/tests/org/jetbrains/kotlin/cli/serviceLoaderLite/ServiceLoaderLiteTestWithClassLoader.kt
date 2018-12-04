/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.serviceLoaderLite

import org.jetbrains.kotlin.cli.jvm.plugins.ServiceLoaderLite
import java.net.URLClassLoader
import kotlin.reflect.KClass

interface Intf
class Component1 : Intf
class Component2 : Intf
class ComponentWithParameters(val a: String) : Intf
class UnrelatedComponent
enum class EnumComponent : Intf

class ServiceLoaderLiteTestWithClassLoader : AbstractServiceLoaderLiteTest() {
    class NestedComponent : Intf
    inner class InnerComponent : Intf

    fun testClassloader1() {
        @Suppress("RemoveExplicitTypeArguments")
        val entries = arrayOf(impls<Intf>(Component1::class, Component2::class), clazz<Component1>(), clazz<Component2>())

        classLoaderTest("test", *entries) { classLoader ->
            val impls = ServiceLoaderLite.loadImplementations<Intf>(classLoader)
            assertTrue(impls.any { it is Component1 })
            assertTrue(impls.any { it is Component2 })
        }
    }

    fun testDirWithSpaces() {
        classLoaderTest("test dir", impls<Intf>(NestedComponent::class), clazz<NestedComponent>()) { classLoader ->
            val impls = ServiceLoaderLite.loadImplementations<Intf>(classLoader)
            assertTrue(impls.single() is NestedComponent)
        }
    }

    fun testNestedComponent() {
        classLoaderTest("test", impls<Intf>(NestedComponent::class), clazz<NestedComponent>()) { classLoader ->
            val impls = ServiceLoaderLite.loadImplementations<Intf>(classLoader)
            assertTrue(impls.single() is NestedComponent)
        }
    }

    fun testInnerComponent() {
        classLoaderTest("test", impls<Intf>(InnerComponent::class), clazz<InnerComponent>()) { classLoader ->
            assertThrows<InstantiationException> {
                ServiceLoaderLite.loadImplementations<Intf>(classLoader)
            }
        }
    }

    fun testComponentWithParameters() {
        classLoaderTest("test", impls<Intf>(ComponentWithParameters::class), clazz<ComponentWithParameters>()) { classLoader ->
            assertThrows<InstantiationException> {
                ServiceLoaderLite.loadImplementations<Intf>(classLoader)
            }
        }
    }

    fun testInterface() {
        @Suppress("RemoveExplicitTypeArguments")
        classLoaderTest("test", impls<Intf>(Intf::class), clazz<Intf>()) { classLoader ->
            assertThrows<InstantiationException> {
                ServiceLoaderLite.loadImplementations<Intf>(classLoader)
            }
        }
    }

    fun testEnum() {
        classLoaderTest("test", impls<Intf>(EnumComponent::class), clazz<EnumComponent>()) { classLoader ->
            assertThrows<InstantiationException> {
                ServiceLoaderLite.loadImplementations<Intf>(classLoader)
            }
        }
    }

    fun testUnrelatedComponent() {
        val implsEntry = Entry("META-INF/services/" + Intf::class.java.name, UnrelatedComponent::class.java.name)
        classLoaderTest("test", implsEntry, clazz<UnrelatedComponent>()) { classLoader ->
            assertThrows<ClassCastException> {
                ServiceLoaderLite.loadImplementations<Intf>(classLoader)
            }
        }
    }

    fun testNestedClassLoaders() {
        val entries1 = arrayOf(impls<Intf>(Component1::class), clazz<Component1>())
        val entries2 = arrayOf(impls<Intf>(Component2::class), clazz<Component2>())

        var index = 0
        classLoaderTest("test" + index++, *entries1) { classLoader1 ->
            val impls1 = ServiceLoaderLite.loadImplementations<Intf>(classLoader1)
            assertTrue(impls1.single() is Component1)

            classLoaderTest("test2" + index++, *entries2, parent = classLoader1) { classLoader2 ->
                val impls2 = ServiceLoaderLite.loadImplementations<Intf>(classLoader2)
                assertTrue(impls2.single() is Component2)
            }
        }
    }

    fun testEmpty() {
        val classLoader = URLClassLoader(emptyArray(), ServiceLoaderLiteTestWithClassLoader::class.java.classLoader)
        val impls = ServiceLoaderLite.loadImplementations<Intf>(classLoader)
        assertTrue(impls.isEmpty())
    }

    private fun classLoaderTest(name: String, vararg entries: Entry, parent: ClassLoader? = null, block: (URLClassLoader) -> Unit) {
        applyForDirAndJar(name, *entries) { file ->
            val parentClassLoader = parent ?: ServiceLoaderLiteTestWithClassLoader::class.java.classLoader
            val classLoader = URLClassLoader(arrayOf(file.toURI().toURL()), parentClassLoader)
            block(classLoader)
        }
    }

    private inline fun <reified T : Any> clazz() = Entry(T::class.java.name.replace('.', '/'), bytecode(T::class.java))

    private fun bytecode(clazz: Class<*>): ByteArray {
        val resourcePath = clazz.name.replace('.', '/') + ".class"
        return clazz.classLoader.getResource(resourcePath).readBytes()
    }

    private inline fun <reified Intf : Any> impls(vararg impls: KClass<out Intf>): Entry {
        val content = buildString {
            for (impl in impls) {
                appendln(impl.java.name)
            }
        }
        return Entry("META-INF/services/" + Intf::class.java.name, content)
    }
}