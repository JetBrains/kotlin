/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.serviceLoaderLite

import org.jetbrains.kotlin.util.ServiceLoaderLite
import org.jetbrains.kotlin.util.ServiceLoaderLite.ServiceLoadingException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import javax.annotation.processing.Processor

class ServiceLoaderLiteTest : AbstractServiceLoaderLiteTest() {
    @Test
    fun testSimple() = applyForDirAndJar("test", processors("test.Foo")) { file ->
        val impls = ServiceLoaderLite.findImplementations<Processor>(listOf(file))
        assertEquals("test.Foo", impls.single())
    }

    @Test
    fun testEmpty() = applyForDirAndJar("test") { file ->
        val impls = ServiceLoaderLite.findImplementations<Processor>(listOf(file))
        assertEquals(0, impls.size)
    }

    @Test
    fun testEmpty2() = applyForDirAndJar("test", Entry("foo", "bar")) { file ->
        val impls = ServiceLoaderLite.findImplementations<Processor>(listOf(file))
        assertEquals(0, impls.size)
    }

    @Test
    fun testEmpty3() = applyForDirAndJar("test", processors("")) { file ->
        val impls = ServiceLoaderLite.findImplementations<Processor>(listOf(file))
        assertEquals(0, impls.size)
    }

    @Test
    fun testSeveralProcessors() {
        val processorsContent = buildString { appendLine("test.Foo").appendLine("test.Bar") }

        applyForDirAndJar("test", processors(processorsContent)) { file ->
            val impls = ServiceLoaderLite.findImplementations(Processor::class.java, listOf(file))
            assertEquals(2, impls.size)
            assertTrue("test.Foo" in impls)
            assertTrue("test.Bar" in impls)
        }
    }

    @Test
    fun testSeveralEntries() = applyForDirAndJar("test", processors("test.Foo"), Entry("foo", "bar")) { file ->
        val impls = ServiceLoaderLite.findImplementations<Processor>(listOf(file))
        assertEquals("test.Foo", impls.single())
    }

    @Test
    fun testSeveralJars() {
        val jar1 = writeJar("test.jar", processors("test.Foo"))
        val jar2 = writeJar("test2.jar", processors("ap.Bar"))

        val impls = ServiceLoaderLite.findImplementations<Processor>(listOf(jar1, jar2))

        assertEquals(2, impls.size)
        assertTrue("test.Foo" in impls)
        assertTrue("ap.Bar" in impls)
    }

    @Test
    fun testSeveralDirs() {
        val dir1 = writeDir("test", processors("test.Foo"))
        val dir2 = writeDir("test2", processors("ap.Bar"))

        val impls = ServiceLoaderLite.findImplementations<Processor>(listOf(dir1, dir2))

        assertEquals(2, impls.size)
        assertTrue("test.Foo" in impls)
        assertTrue("ap.Bar" in impls)
    }

    @Test
    fun testDirAndJar() {
        val jar = writeJar("test", processors("test.Foo"))
        val dir = writeDir("test2", processors("ap.Bar"))

        val impls = ServiceLoaderLite.findImplementations<Processor>(listOf(jar, dir))

        assertEquals(2, impls.size)
        assertTrue("test.Foo" in impls)
        assertTrue("ap.Bar" in impls)
    }

    @Test
    fun testParsingError() {
        applyForDirAndJar("test", processors("5")) { file ->
            assertThrows<ServiceLoadingException> {
                ServiceLoaderLite.findImplementations(Processor::class.java, listOf(file))
            }
        }
    }

    @Test
    fun testParsingError2() {
        applyForDirAndJar("test", processors("a b c")) { file ->
            assertThrows<ServiceLoadingException> {
                ServiceLoaderLite.findImplementations(Processor::class.java, listOf(file))
            }
        }
    }

    @Test
    fun testCommentsAndWhitespaces() {
        val processorsContent = buildString {
            appendLine("  test.Foo #comment")
            appendLine("#comment2")
            appendLine().appendLine()
            appendLine("test.Bar #anotherComemnt")
            appendLine("test.Zoo  ")
        }

        applyForDirAndJar("test", processors(processorsContent)) { file ->
            val impls = ServiceLoaderLite.findImplementations(Processor::class.java, listOf(file))
            assertEquals(3, impls.size)
            assertTrue("test.Foo" in impls)
            assertTrue("test.Bar" in impls)
            assertTrue("test.Zoo" in impls)
        }
    }

    @Test
    fun testWrongJarName() {
        val file = File(tmpdir, "foo.tar.gz")
        file.writeText("foobar")
        ServiceLoaderLite.findImplementations(Processor::class.java, listOf(file))
    }
}

