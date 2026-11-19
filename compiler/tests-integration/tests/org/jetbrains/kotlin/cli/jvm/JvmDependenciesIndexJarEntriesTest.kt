/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.FastJarFileSystem
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.JarArchiveCache
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntry
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntryFactory
import org.jetbrains.kotlin.cli.jvm.index.VfsRootEntry
import org.jetbrains.kotlin.cli.jvm.index.nio.JarClassFileHandle
import org.jetbrains.kotlin.cli.jvm.index.nio.JarRootEntry
import org.jetbrains.kotlin.cli.jvm.index.nio.PathRootEntry
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.outputStream
import kotlin.io.path.writeBytes

/** Roots of a test classpath with NIO entries: directories as [PathRootEntry], jars from FastJar [archives], others with VFS entries. */
internal class NioTestRootEntryFactory(val archives: JarArchiveCache) : JvmRootEntryFactory {
    override fun createRootEntry(root: JavaRoot) = when {
        root.file.fileSystem.protocol == "file" && root.file.isDirectory -> PathRootEntry.root(Paths.get(root.file.path))
        root.file.fileSystem.protocol == "jar" && root.file.parent == null ->
            archives.rootEntry(Paths.get(root.file.path.substringBefore("!/")))
        else -> VfsRootEntry(root.file)
    }
}

/** The root entry of the [jar] archive of this cache. */
internal fun JarArchiveCache.rootEntry(jar: Path): JarRootEntry =
    JarRootEntry.build(this[jar.toAbsolutePath().normalize().toString()])

/** A FastJar file system for a test, whose archives are released by [FastJarFileSystem.clearHandlersCache] after the test. */
internal fun newFastJarFileSystem(): FastJarFileSystem = checkNotNull(FastJarFileSystem.createIfUnmappingPossible())

/** The shared contract over NIO directory and jar entries. */
class JvmDependenciesIndexJarEntriesContractTest : AbstractJvmDependenciesIndexContractTest() {
    private val fileSystem = newFastJarFileSystem()

    @AfterEach
    fun clearArchives() = fileSystem.clearHandlersCache()

    override fun createIndex(roots: List<JavaRoot>): JvmDependenciesIndex =
        JvmDependenciesIndexImpl(roots, NioTestRootEntryFactory(fileSystem.archives))
}

/** Behavior of the jar entries ([JarRootEntry]) over the archives of FastJar. */
class JvmDependenciesIndexJarEntriesTest : JvmDependenciesIndexContractTestBase() {
    private val fileSystem = newFastJarFileSystem()

    private val archives: JarArchiveCache
        get() = fileSystem.archives

    @AfterEach
    fun clearArchives() = fileSystem.clearHandlersCache()

    private fun index(vararg roots: JavaRoot) = JvmDependenciesIndexImpl(roots.asList(), NioTestRootEntryFactory(archives))

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
        val root = archives.rootEntry(lib)
        assertEquals(listOf("a", "x"), root.children.map { it.name })
        // As a FastJar virtual file, a directory with its own entry `a/b/` has an extra child with an empty name.
        val virtualDirectory = checkNotNull(fileSystem.findFileByPath("${lib.toAbsolutePath()}!/a/b"))
        assertEquals(virtualDirectory.children.map { it.name }, root.findByRelativePath("a/b")!!.children.map { it.name })
        assertEquals(listOf("C.class"), root.findByRelativePath("a/b")!!.children.filter { !it.isDirectory }.map { it.name })
    }

    @Test
    fun entriesShowTheTreeOfTheFastJarVirtualFiles() {
        val lib = jar(
            "lib.jar", "p/dir/", "p/A.class", "p/A\$1.class", "q/", "q/r/", "q/r/B.kotlin_module", "META-INF/MANIFEST.MF",
            // FastJar takes an entry without content for a directory.
            content = mapOf("p/Empty.class" to ByteArray(0)),
        )
        val virtualRoot = checkNotNull(fileSystem.findFileByPath("${lib.toAbsolutePath()}!/"))
        assertEquals(virtualRoot.describe(), archives.rootEntry(lib).describe())
    }

    @Test
    fun entriesShareTheArchiveOfTheFastJarFileSystem() {
        val lib = jar("lib.jar", content = mapOf("p/A.class" to byteArrayOf(1, 2)))
        checkNotNull(fileSystem.findFileByPath("${lib.toAbsolutePath()}!/p/A.class"))
        assertSame(archives[lib.toAbsolutePath().normalize().toString()], archives.rootEntry(lib).archive)

        val a = index(lib.jarRoot()).findClassFiles(classId("p/A"), CLASS).single()
        assertArrayEquals(byteArrayOf(1, 2), a.readBytes())
        fileSystem.cleanOpenFilesCache()
        assertArrayEquals(byteArrayOf(1, 2), a.readBytes(), "a handle reads again after FastJar closed its open archives")
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
            listOf(lib.jarRoot()), NioTestRootEntryFactory(newFastJarFileSystem().archives)
        ).findClassFiles(classId("p/A"), CLASS).single()
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun unreadableArchiveHasNoEntries() {
        val broken = tempDir.resolve("broken.jar").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        assertTrue(archives.rootEntry(broken).children.isEmpty())
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

    /** The names of this file and the files below it, with `/` after directories, in the order of their children. */
    private fun VirtualFile.describe(): List<String> =
        listOf(name + if (isDirectory) "/" else "") + children.flatMap { child -> child.describe().map { "$name/$it" } }

    private fun JvmRootEntry.describe(): List<String> =
        listOf(name + if (isDirectory) "/" else "") + children.flatMap { child -> child.describe().map { "$name/$it" } }
}
