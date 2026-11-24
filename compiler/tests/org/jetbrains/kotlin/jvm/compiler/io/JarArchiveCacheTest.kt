/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler.io

import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.FastJarFileSystem
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.JarArchive
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.JarArchiveCache
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.JarTreeFactory
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.ZipEntryDescription
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry

/** Tests of the archives that FastJar shares with readers of jars that do not use virtual files. */
class JarArchiveCacheTest {
    @TempDir
    lateinit var tempDir: File

    private var fileSystem: FastJarFileSystem? = null

    private val archives: JarArchiveCache
        get() = fileSystem!!.archives

    @BeforeEach
    fun setUp() {
        fileSystem = FastJarFileSystem.createIfUnmappingPossible()
        assumeTrue(fileSystem != null)
    }

    @AfterEach
    fun tearDown() {
        fileSystem?.clearHandlersCache()
    }

    private fun jar(name: String, entries: Map<String, String>, manifest: Manifest? = null): File {
        val file = File(tempDir, name)
        val stream = file.outputStream()
        (if (manifest != null) JarOutputStream(stream, manifest) else JarOutputStream(stream)).use { out ->
            for ([path, text] in entries) {
                out.putNextEntry(ZipEntry(path))
                out.write(text.toByteArray())
                out.closeEntry()
            }
        }
        return file
    }

    private class Node(val name: String, val entry: ZipEntryDescription?) {
        var children: List<Node> = emptyList()
    }

    private object NodeFactory : JarTreeFactory<Node> {
        override fun createRoot() = Node("", entry = null)
        override fun createDirectory(name: CharSequence, parent: Node) = Node(name.toString(), entry = null)
        override fun createFile(name: CharSequence, parent: Node, entry: ZipEntryDescription) = Node(name.toString(), entry)
        override fun setChildren(directory: Node, children: List<Node>) {
            directory.children = children
        }
    }

    private fun JarArchive.node(path: String): Node {
        var node = buildTree(NodeFactory)
        for (name in path.split('/')) {
            node = node.children.single { it.name == name }
        }
        return node
    }

    private fun Node.render(): String =
        if (entry != null) name else name + "/" + children.joinToString(",", "(", ")") { it.render() }

    private fun JarArchive.read(path: String): String = String(contentsToByteArray(node(path).entry!!))

    @Test
    fun archiveIsParsedOnceUntilCleared() {
        val path = jar("lib.jar", mapOf("p/A.txt" to "a")).path
        val archive = archives[path]
        assertSame(archive, archives[path])
        archives.clear()
        assertNotSame(archive, archives[path])
    }

    @Test
    fun treeHasAllParentDirectoriesInEntryOrder() {
        val path = jar("lib.jar", mapOf("b/B.txt" to "b", "a/x/A.txt" to "a", "b/C.txt" to "c", "R.txt" to "r")).path
        val archive = archives[path]
        assertEquals("/(b/(B.txt,C.txt),a/(x/(A.txt)),R.txt)", archive.buildTree(NodeFactory).render())
        assertNotSame(archive.buildTree(NodeFactory), archive.buildTree(NodeFactory))
    }

    @Test
    fun virtualFilesUseTheSharedArchives() {
        val path = jar("lib.jar", mapOf("p/A.txt" to "a")).path
        val archive = archives[path]
        val file = fileSystem!!.findFileByPath("$path!/p/A.txt")!!
        assertSame(archive, archives[path])
        assertSame(file, fileSystem!!.findFileByPath("$path!/p/A.txt"))
        assertSame(file.parent, fileSystem!!.findFileByPath("$path!/p"))
        assertEquals("a", String(file.contentsToByteArray()))
        assertEquals("$path!/p/A.txt".replace(File.separatorChar, '/'), file.path)
    }

    @Test
    fun entriesAreReadAgainAfterTheArchiveIsClosed() {
        val jars = (0 until 40).map { jar("lib$it.jar", mapOf("p/A.txt" to "a$it")).path }
        repeat(2) {
            for ([i, path] in jars.withIndex()) {
                assertEquals("a$i", archives[path].read("p/A.txt"))
            }
        }
        archives.closeOpenArchives()
        assertEquals("a0", archives[jars[0]].read("p/A.txt"))
    }

    @Test
    fun archivesReadRepeatedlyStayOpenWhileManyOthersAreReadOnce() {
        val path = jar("lib.jar", mapOf("p/A.txt" to "a", "p/B.txt" to "b")).path
        val archive = archives[path]
        assertEquals("a", archive.read("p/A.txt"))
        assertEquals("a", archive.read("p/A.txt"))
        for (i in 0 until 30) {
            val other = jar("other$i.jar", mapOf("p/A.txt" to "a$i")).path
            assertEquals("a$i", archives[other].read("p/A.txt"))
        }
        // The archive can only be read without its file if it is still open.
        assumeTrue(File(path).delete())
        assertEquals("b", archive.read("p/B.txt"))
    }

    @Test
    fun concurrentReadsOfMoreArchivesThanStayOpen() {
        val jars = (0 until 50).map { i -> jar("lib$i.jar", (0 until 10).associate { "p/F$it.txt" to "$i-$it" }).path }
        val executor = Executors.newFixedThreadPool(8)
        try {
            val results = (0 until 2000).map { n ->
                executor.submit<Boolean> {
                    val i = (n * 7) % jars.size
                    if (n % 100 == 0) archives.closeOpenArchives()
                    archives[jars[i]].read("p/F${n % 10}.txt") == "$i-${n % 10}"
                }
            }
            assertTrue(results.all { it.get(1, TimeUnit.MINUTES) })
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun manifestIsCached() {
        val manifest = Manifest().apply {
            mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            mainAttributes[Attributes.Name("Automatic-Module-Name")] = "auto.lib"
        }
        val path = jar("lib.jar", mapOf("p/A.txt" to "a"), manifest).path
        val archive = archives[path]
        val entry = archive.node("META-INF/MANIFEST.MF").entry!!
        val bytes = archive.contentsToByteArray(entry)
        assertTrue(String(bytes).contains("Automatic-Module-Name: auto.lib"))
        // The cached manifest is read with the central directory, not from the archive.
        assertTrue(File(path).delete())
        assertArrayEquals(bytes, archive.contentsToByteArray(entry))
    }

    @Test
    fun invalidArchiveHasNoEntries() {
        val file = File(tempDir, "broken.jar").apply { writeText("not a zip") }
        val reported = mutableListOf<Path>()
        val cache = JarArchiveCache(unmapBuffer = {}) { invalid, _ -> reported.add(invalid) }
        val archive = cache[file.path]
        assertTrue(archive.buildTree(NodeFactory).children.isEmpty())
        assertEquals(listOf(file.toPath()), reported)
    }
}
