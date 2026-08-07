/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.asBinaryClassFileHandle
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.io.path.writeBytes

/**
 * A [JvmClasspath] restricts a binary lookup by *root* ([contains]), and `BinaryClassFileHandle.isUnder` is
 * the whole of that test. It has to answer as `VfsBasedProjectEnvironment.psiSearchScope` (i.e. `ClassPathScope`)
 * does for the same classpath: for an entry of an archive the archive itself must be the root, for a loose class
 * file any enclosing directory counts.
 *
 * Worth pinning down here, because the compilations that pass a non-empty root list — the incremental output
 * directories and an HMPP fragment's classpath — have no java-direct test coverage.
 */
class ClasspathRestrictionTest {

    @Test
    fun testClassFileInDirectoryRoot(@TempDir tempDir: Path) {
        val outDir = tempDir.resolve("out")
        val classFile = outDir.resolve("pkg/A.class").createParentsAndWrite()
        val handle = handleFor(classFile.asLocalVirtualFile())

        assertTrue(handle.isUnder(outDir), "the class file lies in the classpath root it was written to")
        assertTrue(handle.isUnder(outDir.resolve("pkg")), "any enclosing directory contains it, as for `ClassPathScope`")
        assertTrue(handle.isUnder(tempDir), "an enclosing directory need not be the immediate one")
        assertFalse(handle.isUnder(tempDir.resolve("other")), "a sibling directory does not contain it")
        assertFalse(handle.isUnder(tempDir.resolve("ou")), "a root is not a mere prefix of the path")
    }

    @Test
    fun testClassFileInArchiveRoot(@TempDir tempDir: Path) {
        val jar = tempDir.resolve("lib/lib.jar").createParentsAndWriteJar("pkg/A.class")
        val handle = handleFor(jar.asJarEntryVirtualFile("pkg/A.class"))

        assertTrue(handle.isUnder(jar), "the archive itself is the classpath root of its entries")
        assertFalse(handle.isUnder(tempDir.resolve("lib")), "the directory holding the archive is not a root of its entries")
        assertFalse(handle.isUnder(tempDir), "and neither is any directory above it")
    }

    @Test
    fun testClasspathShapes(@TempDir tempDir: Path) {
        val outDir = tempDir.resolve("out")
        val inOutDir = handleFor(outDir.resolve("pkg/A.class").createParentsAndWrite().asLocalVirtualFile())
        val libDir = tempDir.resolve("lib")
        val inLibDir = handleFor(libDir.resolve("pkg/B.class").createParentsAndWrite().asLocalVirtualFile())

        JvmClasspath.Roots(listOf(outDir)).let { classpath ->
            assertTrue(inOutDir in classpath)
            assertFalse(inLibDir in classpath)
        }
        JvmClasspath.Roots(listOf(libDir, outDir)).let { classpath ->
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
        JvmClasspath.ProjectLibraries(excludedRoots = listOf(outDir)).let { classpath ->
            assertFalse(inOutDir in classpath, "an excluded root is the incremental-compilation output directory")
            assertTrue(inLibDir in classpath)
        }
    }

    private fun handleFor(classFile: VirtualFile): BinaryClassFileHandle = classFile.asBinaryClassFileHandle()

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
