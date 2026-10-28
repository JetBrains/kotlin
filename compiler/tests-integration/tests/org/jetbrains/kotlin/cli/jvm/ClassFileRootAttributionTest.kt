/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.jvm.compiler.CliVirtualFileFinderFactory
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtension
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtensions
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.cli.jvm.index.asBinaryClassFileHandle
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.disposeRootDisposable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.io.path.writeBytes

/**
 * A class file found through the CLI index carries the identity of the root it was found in, assigned by the index
 * rather than inferred from the file's path, and the handle built over it answers in terms of that root.
 */
class ClassFileRootAttributionTest {
    private val testRootDisposable: Disposable = Disposer.newDisposable()

    @AfterEach
    fun tearDown() {
        disposeRootDisposable(testRootDisposable)
        // Opened archives are cached process-wide, which would keep `TempDir` from deleting them on Windows.
        ZipHandler.clearFileAccessorCache()
    }

    @Test
    fun testClassFileKnowsItsRoot(@TempDir tempDir: Path) {
        val outDir = tempDir.resolve("out").createDirectories()
        outDir.resolve("pkg/A.class").createParentsAndWrite()
        outDir.resolve("pkg/A\$B.class").createParentsAndWrite()
        outDir.resolve("pkg/A\$1.class").createParentsAndWrite()
        val jar = tempDir.resolve("lib/lib.jar").createParentsAndWriteJar("pkg/A.class", "pkg/C.class")

        val dirRoot = JavaRoot(outDir.asLocalVirtualFile(), JavaRoot.RootType.BINARY)
        val jarRoot = JavaRoot(jar.asJarRootVirtualFile(), JavaRoot.RootType.BINARY)
        val index = JvmDependenciesIndexImpl(listOf(dirRoot, jarRoot))

        val a = index.findClassFiles(ClassId.fromString("pkg/A"), CLASS_FILES).single()
        assertEquals(dirRoot, a.root, "the first root on the classpath wins")
        assertEquals(JvmClasspathRootId.of(outDir), a.root.id)
        assertEquals(outDir, a.root.libraryPath)

        val c = index.findClassFiles(ClassId.fromString("pkg/C"), CLASS_FILES).single()
        assertEquals(jarRoot, c.root)
        assertEquals(JvmClasspathRootId.of(jar), c.root.id, "the archive is the root of its entries")
        assertEquals(jar, c.root.libraryPath)

        val handle = a.asBinaryClassFileHandle()
        assertEquals(dirRoot.id, handle.rootId)
        assertEquals(outDir, handle.rootPath)
        assertEquals(a.file.path, handle.pathForDiagnostics)
        assertEquals("A", handle.nameWithoutExtension)
        assertEquals("class", handle.extension)

        val nested = checkNotNull(handle.sibling("A\$B.class")) { "the class file of a nested class lies next to its outer class" }
        assertEquals(dirRoot.id, nested.rootId, "a sibling belongs to the same root")
        assertEquals("A\$B", nested.nameWithoutExtension)
        assertNotNull(handle.sibling("A\$1.class")) { "a literal `$` in a file name is a sibling like any other" }
        assertNull(handle.sibling("A\$Missing.class"))

        val inJar = c.asBinaryClassFileHandle()
        assertEquals(jar.pathForVfs() + JAR_SEPARATOR + "pkg/C.class", inJar.pathForDiagnostics)
        assertEquals(jarRoot.id, checkNotNull(inJar.sibling("A.class")).rootId)
    }

    /**
     * Every `ct.sym` module root of `-Xjdk-release` reports the same virtual file path, so their identity is a synthetic
     * id assigned when the module is created, and shared by the `JavaModule.Root` and the `JavaRoot` built from it.
     */
    @Test
    fun testCtSymRootsHaveDistinctIdentities() {
        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.JDK_ONLY, TestJdkKind.FULL_JDK)
        configuration.put(JVMConfigurationKeys.JDK_RELEASE, 11)

        @OptIn(CoreEnvironmentDeprecation::class)
        val environment = KotlinCoreEnvironment.createForParallelTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val index = (environment.project.getService(VirtualFileFinderFactory::class.java) as CliVirtualFileFinderFactory).index

        val signatureRoots = index.indexedRoots.filter { it.type == JavaRoot.RootType.BINARY_SIG }.toList()
        assertTrue(signatureRoots.size > 1, "several JDK modules are expected on the module graph: $signatureRoots")
        assertEquals(signatureRoots.size, signatureRoots.mapTo(HashSet()) { it.id }.size, "every ct.sym root has its own id")
        assertEquals(1, signatureRoots.mapTo(HashSet()) { it.file.path }.size, "...although they all share one path")

        val objectClass = index.findClassFiles(ClassId.fromString("java/lang/Object"), SIG_FILES).single()
        val javaBase = signatureRoots.single { it.id.id.endsWith("/java.base") }
        assertEquals(javaBase, objectClass.root)
    }

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

    private fun Path.asJarRootVirtualFile(): VirtualFile {
        val path = pathForVfs() + JAR_SEPARATOR
        return checkNotNull(CoreJarFileSystem().findFileByPath(path)) { "not in the VFS: $path" }
    }

    private fun Path.pathForVfs(): String = toAbsolutePath().normalize().toString().replace('\\', '/')

    private companion object {
        val CLASS_FILES = JavaFileExtensions(JavaFileExtension.CLASS)
        val SIG_FILES = JavaFileExtensions(JavaFileExtension.SIG)
    }
}
