/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.cli.jvm.index.nio.JarClassFileHandle
import org.jetbrains.kotlin.cli.jvm.index.nio.JarFilePool
import org.jetbrains.kotlin.cli.jvm.index.nio.NioRootEntryFactory
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.outputStream
import kotlin.io.path.writeBytes

/** Behavior of the jar entries ([org.jetbrains.kotlin.cli.jvm.index.nio.JarRootEntry]) and their [JarFilePool]. */
class JvmDependenciesIndexJarEntriesTest : JvmDependenciesIndexContractTestBase() {
    private val pool = checkNotNull(JarFilePool.createIfUnmappingPossible())

    @AfterEach
    fun closePool() = pool.close()

    private fun index(vararg roots: JavaRoot) = JvmDependenciesIndexImpl(roots.asList(), NioRootEntryFactory(pool))

    @Test
    fun readsDeflatedAndStoredEntries() {
        val bytes = ByteArray(10_000) { (it % 7).toByte() }
        val lib = jar("lib.jar", content = mapOf("p/A.class" to bytes))
        val stored = tempDir.resolve("stored.jar").also { writeStoredJar(it, "p/B.class", bytes) }
        val index = index(lib.jarRoot(), stored.jarRoot())

        val a = index.findClassFiles(classId("p/A"), CLASS).single()
        assertInstanceOf(JarClassFileHandle::class.java, a)
        assertArrayEquals(bytes, a.readBytes())
        assertEquals(JvmClasspathRootId.of(lib), a.rootId)
        assertTrue(a.pathForDiagnostics.endsWith("lib.jar!/p/A.class"), a.pathForDiagnostics)
        assertArrayEquals(bytes, index.findClassFiles(classId("p/B"), CLASS).single().readBytes())
    }

    @Test
    fun parentDirectoriesExistWithAndWithoutTheirOwnEntries() {
        val lib = jar("lib.jar", "a/", "a/b/", "a/b/C.class", "x/y/Z.class")
        val index = index(lib.jarRoot())
        assertTrue(index.hasPackage(FqName("a.b")))
        assertTrue(index.hasPackage(FqName("x.y")))
        assertNotNull(index.findClassFiles(classId("x/y/Z"), CLASS).singleOrNull())
        val root = pool.rootEntry(lib)
        assertEquals(listOf("a", "x"), root.children.map { it.name })
        assertEquals(listOf("C.class"), root.findByRelativePath("a/b")!!.children.map { it.name })
    }

    @Test
    fun emptyFilesAreFilesAndDirectoriesNeedASlash() {
        val lib = jar("lib.jar", "p/Empty.class", "p/dir/")
        val root = pool.rootEntry(lib)
        assertFalse(root.findByRelativePath("p/Empty.class")!!.isDirectory)
        assertTrue(root.findByRelativePath("p/dir")!!.isDirectory)
    }

    @Test
    fun handleFindsSiblingFilesOnly() {
        val lib = jar("lib.jar", "p/A.class", "p/A\$1.class", "p/A\$sub/X.class")
        val a = index(lib.jarRoot()).findClassFiles(classId("p/A"), CLASS).single()
        assertEquals("A\$1.class", a.sibling("A\$1.class")?.let { "${it.nameWithoutExtension}.${it.extension}" })
        assertNull(a.sibling("A\$sub"))
        assertNull(a.sibling("a\$1.class"))
    }

    @Test
    fun handlesAreEqualByRootAndPathInTheArchive() {
        val lib = jar("lib.jar", "p/A.class")
        val first = index(lib.jarRoot()).findClassFiles(classId("p/A"), CLASS).single()
        val second = JvmDependenciesIndexImpl(
            listOf(lib.jarRoot()), NioRootEntryFactory(checkNotNull(JarFilePool.createIfUnmappingPossible()))
        ).findClassFiles(classId("p/A"), CLASS).single()
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun centralDirectoryIsReadOnceAndArchivesAreReopenedAfterClose() {
        val lib = jar("lib.jar", content = mapOf("p/A.class" to byteArrayOf(1, 2)))
        assertTrue(pool.rootEntry(lib) === pool.rootEntry(lib))
        assertEquals(0, pool.openFileCount, "reading the central directory keeps no archive open")

        val a = index(lib.jarRoot()).findClassFiles(classId("p/A"), CLASS).single()
        a.readBytes()
        assertEquals(1, pool.openFileCount)
        pool.close()
        pool.close()
        assertEquals(0, pool.openFileCount)
        assertArrayEquals(byteArrayOf(1, 2), a.readBytes(), "a handle reads again after its pool released the archive")
        assertEquals(1, pool.openFileCount)
    }

    @Test
    fun atMostTwentyArchivesStayOpen() {
        val handles = (1..25).map { i ->
            val lib = jar("lib$i.jar", content = mapOf("p/A$i.class" to byteArrayOf(i.toByte())))
            index(lib.jarRoot()).findClassFiles(classId("p/A$i"), CLASS).single()
        }
        handles.forEachIndexed { i, handle -> assertArrayEquals(byteArrayOf((i + 1).toByte()), handle.readBytes()) }
        assertEquals(20, pool.openFileCount)
    }

    @Test
    fun unreadableArchiveHasNoEntries() {
        val broken = tempDir.resolve("broken.jar").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        assertTrue(pool.rootEntry(broken).children.isEmpty())
    }

    @Test
    fun readsZip64Archives() {
        val lib = tempDir.resolve("zip64.jar")
        ZipOutputStream(lib.outputStream()).use { out ->
            for (i in 0 until 70_000) {
                out.putNextEntry(ZipEntry("p/C$i.class"))
                out.write(i.toString().toByteArray())
                out.closeEntry()
            }
        }
        val index = index(lib.jarRoot())
        assertEquals("69999", String(index.findClassFiles(classId("p/C69999"), CLASS).single().readBytes()))
        var count = 0
        index.traverseFilesInPackage(FqName("p"), CLASS) { count++; true }
        assertEquals(70_000, count)
    }

    private fun writeStoredJar(path: Path, name: String, bytes: ByteArray) {
        ZipOutputStream(path.outputStream()).use { out ->
            out.putNextEntry(ZipEntry(name).apply {
                method = ZipEntry.STORED
                size = bytes.size.toLong()
                crc = CRC32().apply { update(bytes) }.value
            })
            out.write(bytes)
            out.closeEntry()
        }
    }
}
