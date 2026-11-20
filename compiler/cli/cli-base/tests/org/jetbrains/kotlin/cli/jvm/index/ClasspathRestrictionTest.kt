/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.jvm.environment.asJvmClasspathRootId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.io.path.writeBytes

/**
 * A [JvmClasspath] restricts a binary lookup by indexed root. Directory roots use path-component containment; an archive entry is
 * contained only by that archive root, not by the directory holding the archive.
 *
 * The classpaths below are those of an incremental compilation, the only compilation that gives a session a
 * proper part of the classpath: the output directory of the previous build, as the roots of the
 * precompiled-binaries session and as the exclusions of the libraries session. (An HMPP fragment narrows the
 * *Kotlin* class finder only; `createBinaryJavaFacade` is always given the whole libraries classpath.)
 *
 * What the restriction then decides, in a compilation which actually runs that shape, is in
 * `org.jetbrains.kotlin.incremental.IncrementalJavaClassFromPreviousOutputTest`: a Java class file left in the
 * output directory by the previous build is read by the precompiled-binaries session, while the reference its
 * signature records is resolved on the whole classpath. The incremental suites do not observe it, because their
 * fixtures state Java as sources, which are compiled outside the classpath of the Kotlin compilation.
 */
class ClasspathRestrictionTest {

    /**
     * `CoreJarFileSystem`/`ZipHandler` cache an opened archive's file handle process-wide, well past the
     * `TempDir` this test wrote it under going out of scope. Unix tolerates deleting a file that is still
     * open; Windows does not, so a leftover handle turns `TempDir`'s post-test cleanup into a "used by
     * another process" failure. Releasing the cache once a test is done keeps that cleanup from racing it.
     */
    @AfterEach
    fun clearJarHandleCaches() {
        ZipHandler.clearFileAccessorCache()
    }

    @Test
    fun testClassFileInDirectoryRoot(@TempDir tempDir: Path) {
        val outDir = tempDir.resolve("out")
        outDir.resolve("pkg/A.class").createParentsAndWrite()
        val classpath = JvmClasspath.Roots(rootIds(outDir))

        assertTrue(classpath.containsRoot(outDir.asLocalVirtualFile()), "the classpath root is accepted")
        assertTrue(classpath.containsRoot(outDir.resolve("pkg").asLocalVirtualFile()), "nested indexed roots are accepted")
        assertTrue(JvmClasspath.Roots(rootIds(tempDir)).containsRoot(outDir.asLocalVirtualFile()), "an ancestor root is accepted")
        assertFalse(classpath.containsRoot(tempDir.resolve("other").createDirectories().asLocalVirtualFile()), "a sibling is not accepted")
        assertFalse(
            JvmClasspath.Roots(rootIds(tempDir.resolve("ou").createDirectories())).containsRoot(outDir.asLocalVirtualFile()),
            "a root is not a mere path prefix",
        )
    }

    @Test
    fun testClassFileInArchiveRoot(@TempDir tempDir: Path) {
        val jar = tempDir.resolve("lib/lib.jar").createParentsAndWriteJar("pkg/A.class")
        val jarRoot = checkNotNull(CoreJarFileSystem().findFileByPath(jar.pathForVfs() + JAR_SEPARATOR))

        assertTrue(JvmClasspath.Roots(rootIds(jar)).containsRoot(jarRoot), "the archive itself is the classpath root of its entries")
        assertFalse(
            JvmClasspath.Roots(rootIds(tempDir.resolve("lib"))).containsRoot(jarRoot),
            "the directory holding the archive is not a root of its entries",
        )
        assertFalse(JvmClasspath.Roots(rootIds(tempDir)).containsRoot(jarRoot), "neither is any directory above it")
    }

    @Test
    fun testClasspathShapes(@TempDir tempDir: Path) {
        val outDir = tempDir.resolve("out")
        outDir.resolve("pkg/A.class").createParentsAndWrite()
        val inOutDir = outDir.asLocalVirtualFile()
        val libDir = tempDir.resolve("lib")
        libDir.resolve("pkg/B.class").createParentsAndWrite()
        val inLibDir = libDir.asLocalVirtualFile()

        JvmClasspath.Roots(rootIds(outDir)).let { classpath ->
            assertTrue(classpath.containsRoot(inOutDir))
            assertFalse(classpath.containsRoot(inLibDir))
        }
        JvmClasspath.Roots(rootIds(libDir, outDir)).let { classpath ->
            assertTrue(classpath.containsRoot(inOutDir), "any of the roots is enough")
            assertTrue(classpath.containsRoot(inLibDir))
        }
        JvmClasspath.Roots(emptyList()).let { classpath ->
            assertFalse(classpath.containsRoot(inOutDir), "an empty classpath contains nothing")
        }
        // Everything an index yields is on the classpath of the compilation, so the whole classpath contains it.
        JvmClasspath.ProjectLibraries().let { classpath ->
            assertTrue(classpath.containsRoot(inOutDir))
            assertTrue(classpath.containsRoot(inLibDir))
        }
        JvmClasspath.ProjectLibraries(excludedRoots = rootIds(outDir)).let { classpath ->
            assertFalse(classpath.containsRoot(inOutDir), "an excluded root is the incremental-compilation output directory")
            assertTrue(classpath.containsRoot(inLibDir))
        }
    }

    /**
     * A root has two spellings — a path on the compiler's classpath and a virtual file — and
     * [JvmClasspathRootId] has to be the same for both, or a classpath named by the caller would not match the
     * roots the compilation indexed.
     */
    @Test
    fun testRootIdentityIsTheSameForAPathAndAVirtualFile(@TempDir tempDir: Path) {
        val dir = tempDir.resolve("out").apply { createDirectories() }
        assertEquals(JvmClasspathRootId.of(dir), dir.asLocalVirtualFile().asJvmClasspathRootId())

        val jar = tempDir.resolve("lib/lib.jar").createParentsAndWriteJar("pkg/A.class")
        val jarRoot = checkNotNull(CoreJarFileSystem().findFileByPath(jar.pathForVfs() + JAR_SEPARATOR))
        assertEquals(JvmClasspathRootId.of(jar), jarRoot.asJvmClasspathRootId())
    }

    @Test
    fun testPathIdentityNormalizationAndCase(@TempDir tempDir: Path) {
        val root = tempDir.resolve("out")
        val rootId = JvmClasspathRootId.of(root)

        assertEquals(rootId, JvmClasspathRootId.of(root.resolve("nested/..")))
        assertEquals(rootId, JvmClasspathRootId.of(root.fileSystem.getPath("${root}/")))
        assertNotEquals(JvmClasspathRootId.of(tempDir.resolve("CaseRoot")), JvmClasspathRootId.of(tempDir.resolve("caseroot")))
    }

    @Test
    fun testSymbolicLinkHasDistinctRootIdentity(@TempDir tempDir: Path) {
        val target = tempDir.resolve("target").createDirectories()
        val link = tempDir.resolve("link")
        val symbolicLinkResult = runCatching { Files.createSymbolicLink(link, target) }
        assumeTrue(symbolicLinkResult.isSuccess)

        assertNotEquals(JvmClasspathRootId.of(target), JvmClasspathRootId.of(link))
    }

    @Test
    fun testClasspathRootMatching(@TempDir tempDir: Path) {
        val listedDirectory = tempDir.resolve("out").createDirectories()
        val nestedDirectory = listedDirectory.resolve("nested").createDirectories()
        val jar = listedDirectory.resolve("lib.jar").createParentsAndWriteJar("pkg/A.class")
        val jarRoot = checkNotNull(CoreJarFileSystem().findFileByPath(jar.pathForVfs() + JAR_SEPARATOR))
        val classpath = JvmClasspath.Roots(rootIds(listedDirectory))

        assertTrue(classpath.containsRoot(listedDirectory.asLocalVirtualFile()))
        assertTrue(classpath.containsRoot(nestedDirectory.asLocalVirtualFile()))
        assertFalse(classpath.containsRoot(jarRoot), "a jar under a listed directory is a different classpath root")
        assertTrue(JvmClasspath.Roots(rootIds(jar)).containsRoot(jarRoot))

        val libraries = JvmClasspath.ProjectLibraries(rootIds(listedDirectory))
        assertFalse(libraries.containsRoot(nestedDirectory.asLocalVirtualFile()))
        assertTrue(libraries.containsRoot(jarRoot))

        val redundantSeparatorId = JvmClasspathRootId("${listedDirectory}//")
        assertTrue(
            JvmClasspath.Roots(listOf(redundantSeparatorId)).containsRoot(nestedDirectory.asLocalVirtualFile()),
            "Path matching handles redundant separators",
        )
    }

    @Test
    fun testClasspathRootMatchingUsesPathCaseRulesOnWindows(@TempDir tempDir: Path) {
        assumeTrue(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))

        val root = tempDir.resolve("out").createDirectories()
        val caseVariant = JvmClasspathRootId(root.resolveSibling("OUT").toString())

        assertNotEquals(JvmClasspathRootId.of(root), caseVariant, "root ID identity remains string-based")
        assertTrue(JvmClasspath.Roots(listOf(caseVariant)).containsRoot(root.asLocalVirtualFile()))
    }

    @Test
    fun testSyntheticRootIdDoesNotMatchALocalRoot(@TempDir tempDir: Path) {
        val onDiskRoot = tempDir.asLocalVirtualFile()
        val synthetic = JvmClasspathRootId("__module_in-memory__output__")

        assertFalse(JvmClasspath.Roots(listOf(synthetic)).containsRoot(onDiskRoot))
        assertTrue(JvmClasspath.ProjectLibraries(excludedRoots = listOf(synthetic)).containsRoot(onDiskRoot))
    }

    private fun rootIds(vararg roots: Path): List<JvmClasspathRootId> = roots.map(JvmClasspathRootId::of)

    private fun Path.createParentsAndWrite(): Path = apply {
        parent.createDirectories()
        writeBytes(ByteArray(0))
    }

    private fun Path.createParentsAndWriteJar(vararg entries: String): Path = apply {
        parent.createDirectories()
        ZipOutputStream(outputStream()).use { out ->
            for (entry in entries) {
                out.putNextEntry(ZipEntry(entry))
                out.closeEntry()
            }
        }
    }

    private fun Path.asLocalVirtualFile(): VirtualFile =
        checkNotNull(CoreLocalFileSystem().findFileByPath(pathForVfs())) { "not in the VFS: " + pathForVfs() }

    private fun Path.pathForVfs(): String = toAbsolutePath().normalize().toString().replace('\\', '/')
}
