/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.jvm.environment.asJvmClasspathRootId
import org.jetbrains.kotlin.jvm.environment.containsFile
import org.jetbrains.kotlin.jvm.environment.containsRoot
import org.jetbrains.kotlin.jvm.environment.isUnder
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.contains
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
 * A [JvmClasspath] restricts a binary lookup by *root* ([contains]): a class file belongs to the root the index
 * found it in ([BinaryClassFileHandle.rootId]), and the restriction is the identity of that root. A *virtual file*
 * is matched by path instead ([containsFile]), as `VfsBasedProjectEnvironment.psiSearchScope` (i.e. `ClassPathScope`)
 * does: for an entry of an archive the archive itself must be the root, for a loose class file any enclosing
 * directory counts.
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
        val classFile = outDir.resolve("pkg/A.class").createParentsAndWrite().asLocalVirtualFile()

        assertTrue(classFile.isUnder(outDir), "the class file lies in the classpath root it was written to")
        assertTrue(classFile.isUnder(outDir.resolve("pkg")), "any enclosing directory contains it, as for `ClassPathScope`")
        assertTrue(classFile.isUnder(tempDir), "an enclosing directory need not be the immediate one")
        assertFalse(classFile.isUnder(tempDir.resolve("other")), "a sibling directory does not contain it")
        assertFalse(classFile.isUnder(tempDir.resolve("ou")), "a root is not a mere prefix of the path")
    }

    @Test
    fun testClassFileInArchiveRoot(@TempDir tempDir: Path) {
        val jar = tempDir.resolve("lib/lib.jar").createParentsAndWriteJar("pkg/A.class")
        val classFile = jar.asJarEntryVirtualFile("pkg/A.class")

        assertTrue(classFile.isUnder(jar), "the archive itself is the classpath root of its entries")
        assertFalse(classFile.isUnder(tempDir.resolve("lib")), "the directory holding the archive is not a root of its entries")
        assertFalse(classFile.isUnder(tempDir), "and neither is any directory above it")
    }

    /**
     * The root of a class file is the one the index found it in, not any directory which happens to enclose it:
     * the roots of a classpath are the entries of the compiler's classpath, and the index is built from exactly those.
     */
    @Test
    fun testHandleBelongsToTheRootTheIndexAssigned(@TempDir tempDir: Path) {
        val outDir = tempDir.resolve("out")
        val handle = handleFor(outDir.resolve("pkg/A.class"), root = outDir)

        assertTrue(handle.isUnder(outDir))
        assertFalse(handle.isUnder(outDir.resolve("pkg")), "a sub-directory of the root is not a root of the index")
        assertFalse(handle.isUnder(tempDir), "and neither is a directory above it")
    }

    @Test
    fun testClasspathShapes(@TempDir tempDir: Path) {
        val outDir = tempDir.resolve("out")
        val inOutDir = handleFor(outDir.resolve("pkg/A.class"), root = outDir)
        val libDir = tempDir.resolve("lib")
        val inLibDir = handleFor(libDir.resolve("pkg/B.class"), root = libDir)

        JvmClasspath.Roots(rootIds(outDir)).let { classpath ->
            assertTrue(inOutDir in classpath)
            assertFalse(inLibDir in classpath)
        }
        JvmClasspath.Roots(rootIds(libDir, outDir)).let { classpath ->
            assertTrue(inOutDir in classpath, "any of the roots is enough")
            assertTrue(inLibDir in classpath)
        }
        JvmClasspath.Roots(emptyList()).let { classpath ->
            assertFalse(inOutDir in classpath, "an empty classpath contains nothing")
        }
        // Everything an index yields is on the classpath of the compilation, so the whole classpath contains it.
        JvmClasspath.ProjectLibraries().let { classpath ->
            assertTrue(inOutDir in classpath)
            assertTrue(inLibDir in classpath)
        }
        JvmClasspath.ProjectLibraries(excludedRoots = rootIds(outDir)).let { classpath ->
            assertFalse(inOutDir in classpath, "an excluded root is the incremental-compilation output directory")
            assertTrue(inLibDir in classpath)
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
    }

    @Test
    fun testClasspathContainsFile(@TempDir tempDir: Path) {
        val listedDirectory = tempDir.resolve("out").createDirectories()
        val nestedClassFile = listedDirectory.resolve("pkg/A.class").createParentsAndWrite().asLocalVirtualFile()
        val excluded = JvmClasspathRootId.of(listedDirectory)

        assertTrue(JvmClasspath.Roots(listOf(excluded)).containsFile(nestedClassFile))
        assertFalse(JvmClasspath.ProjectLibraries(listOf(excluded)).containsFile(nestedClassFile))
    }

    /**
     * A root does not have to be a location in a file system: a build system may compile against an output of
     * its own which was never written to disk (the IntelliJ one does). Such a root takes part in the
     * restriction like any other, and nothing tries to resolve it.
     */
    @Test
    fun testRootOutsideAnyFileSystem(@TempDir tempDir: Path) {
        val onDisk = handleFor(tempDir.resolve("pkg/A.class"), root = tempDir)
        val synthetic = JvmClasspathRootId("__module_in-memory__output__")

        assertFalse(onDisk in JvmClasspath.Roots(listOf(synthetic)))
        assertTrue(onDisk in JvmClasspath.ProjectLibraries(excludedRoots = listOf(synthetic)))
    }

    /** A handle as the CLI index would hand it out for [classFile] found in [root]. */
    private fun handleFor(classFile: Path, root: Path): BinaryClassFileHandle = object : BinaryClassFileHandle {
        override val nameWithoutExtension: String get() = classFile.fileName.toString().substringBeforeLast('.')
        override val extension: String get() = classFile.fileName.toString().substringAfterLast('.')
        override val rootId: JvmClasspathRootId get() = JvmClasspathRootId.of(root)
        override val rootPath: Path get() = root
        override val pathForDiagnostics: String get() = classFile.pathForVfs()
        override fun readBytes(): ByteArray = ByteArray(0)
        override fun sibling(nameWithExtension: String): BinaryClassFileHandle? = null
    }

    private fun BinaryClassFileHandle.isUnder(classpathRoot: Path): Boolean = isUnder(JvmClasspathRootId.of(classpathRoot))

    private fun VirtualFile.isUnder(classpathRoot: Path): Boolean = isUnder(JvmClasspathRootId.of(classpathRoot))

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

    private fun Path.asJarEntryVirtualFile(entry: String): VirtualFile {
        val path = pathForVfs() + JAR_SEPARATOR + entry
        return checkNotNull(CoreJarFileSystem().findFileByPath(path)) { "not in the VFS: " + path }
    }

    private fun Path.pathForVfs(): String = toAbsolutePath().normalize().toString().replace('\\', '/')
}
