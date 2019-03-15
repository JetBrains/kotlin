/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.serviceLoaderLite

import org.jetbrains.kotlin.cli.jvm.plugins.ServiceLoaderLite
import org.jetbrains.kotlin.cli.jvm.plugins.ServiceLoaderLite.ServiceLoadingException
import java.io.File
import javax.annotation.processing.Processor

class ServiceLoaderLiteTest : AbstractServiceLoaderLiteTest() {
    fun testSimple() = applyForDirAndJar("test", processors("test.Foo")) { file ->
        val impls = ServiceLoaderLite.findImplementations<Processor>(listOf(file))
        assertEquals("test.Foo", impls.single())
    }

    fun testEmpty() = applyForDirAndJar("test") { file ->
        val impls = ServiceLoaderLite.findImplementations<Processor>(listOf(file))
        assertEquals(0, impls.size)
    }

    fun testEmpty2() = applyForDirAndJar("test", Entry("foo", "bar")) { file ->
        val impls = ServiceLoaderLite.findImplementations<Processor>(listOf(file))
        assertEquals(0, impls.size)
    }

    fun testEmpty3() = applyForDirAndJar("test", processors("")) { file ->
        val impls = ServiceLoaderLite.findImplementations<Processor>(listOf(file))
        assertEquals(0, impls.size)
    }

    fun testSeveralProcessors() {
        val processorsContent = buildString { appendln("test.Foo").appendln("test.Bar") }

        applyForDirAndJar("test", processors(processorsContent)) { file ->
            val impls = ServiceLoaderLite.findImplementations(Processor::class.java, listOf(file))
            assertEquals(2, impls.size)
            assertTrue("test.Foo" in impls)
            assertTrue("test.Bar" in impls)
        }
    }

    fun testSeveralEntries() = applyForDirAndJar("test", processors("test.Foo"), Entry("foo", "bar")) { file ->
        val impls = ServiceLoaderLite.findImplementations<Processor>(listOf(file))
        assertEquals("test.Foo", impls.single())
    }

    fun testSeveralJars() {
        val jar1 = writeJar("test.jar", processors("test.Foo"))
        val jar2 = writeJar("test2.jar", processors("ap.Bar"))

        val impls = ServiceLoaderLite.findImplementations<Processor>(listOf(jar1, jar2))

        assertEquals(2, impls.size)
        assertTrue("test.Foo" in impls)
        assertTrue("ap.Bar" in impls)
    }

    fun testSeveralDirs() {
        val dir1 = writeDir("test", processors("test.Foo"))
        val dir2 = writeDir("test2", processors("ap.Bar"))

        val impls = ServiceLoaderLite.findImplementations<Processor>(listOf(dir1, dir2))

        assertEquals(2, impls.size)
        assertTrue("test.Foo" in impls)
        assertTrue("ap.Bar" in impls)
    }

    fun testDirAndJar() {
        val jar = writeJar("test", processors("test.Foo"))
        val dir = writeDir("test2", processors("ap.Bar"))

        val impls = ServiceLoaderLite.findImplementations<Processor>(listOf(jar, dir))

        assertEquals(2, impls.size)
        assertTrue("test.Foo" in impls)
        assertTrue("ap.Bar" in impls)
    }

    fun testParsingError() {
        applyForDirAndJar("test", processors("5")) { file ->
            assertThrows<ServiceLoadingException> {
                ServiceLoaderLite.findImplementations(Processor::class.java, listOf(file))
            }
        }
    }

    fun testParsingError2() {
        applyForDirAndJar("test", processors("a b c")) { file ->
            assertThrows<ServiceLoadingException> {
                ServiceLoaderLite.findImplementations(Processor::class.java, listOf(file))
            }
        }
    }

    fun testCommentsAndWhitespaces() {
        val processorsContent = buildString {
            appendln("  test.Foo #comment")
            appendln("#comment2")
            appendln().appendln()
            appendln("test.Bar #anotherComemnt")
            appendln("test.Zoo  ")
        }

        applyForDirAndJar("test", processors(processorsContent)) { file ->
            val impls = ServiceLoaderLite.findImplementations(Processor::class.java, listOf(file))
            assertEquals(3, impls.size)
            assertTrue("test.Foo" in impls)
            assertTrue("test.Bar" in impls)
            assertTrue("test.Zoo" in impls)
        }
    }

    fun testWrongJarName() {
        val file = File(tmpdir, "foo.tar.gz")
        file.writeText("foobar")
        ServiceLoaderLite.findImplementations(Processor::class.java, listOf(file))
    }
}

