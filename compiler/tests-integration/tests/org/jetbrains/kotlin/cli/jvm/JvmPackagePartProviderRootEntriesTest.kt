/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.index.nio.JarFilePool
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.useNioJvmDependenciesIndex
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.disposeRootDisposable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.createDirectories

/** `.kotlin_module` mappings are read the same through VFS and NIO root entries. */
class JvmPackagePartProviderRootEntriesTest {
    @TempDir
    lateinit var tempDir: Path

    private class PackageParts(val parts: List<String>, val metadataParts: List<String>, val packageNames: Set<String>)

    private fun readPackageParts(root: Path, useNioEntries: Boolean, check: () -> Unit = {}): PackageParts {
        val configuration = KotlinTestUtils.newConfiguration().apply {
            put(JVMConfigurationKeys.NO_JDK, true)
            useNioJvmDependenciesIndex = useNioEntries
            addJvmClasspathRoot(root.toFile())
        }
        val disposable = Disposer.newDisposable()
        try {
            @OptIn(CoreEnvironmentDeprecation::class)
            val environment =
                KotlinCoreEnvironment.create(disposable, configuration)
            val provider = environment.createPackagePartProvider(JvmClasspath.ProjectLibraries())
            check()
            return PackageParts(
                provider.findPackageParts("kotlin.collections"),
                provider.findMetadataPackageParts("kotlin.collections"),
                provider.computePackageSetWithNonClassDeclarations(),
            )
        } finally {
            disposeRootDisposable(disposable)
        }
    }

    private fun assertSameParts(root: Path, checkNio: () -> Unit = {}) {
        val vfs = readPackageParts(root, useNioEntries = false)
        val nio = readPackageParts(root, useNioEntries = true, checkNio)
        assertTrue(vfs.parts.isNotEmpty() && "kotlin.collections" in vfs.packageNames)
        assertEquals(vfs.parts, nio.parts)
        assertEquals(vfs.metadataParts, nio.metadataParts)
        assertEquals(vfs.packageNames, nio.packageNames)
    }

    private fun stdlibCopy(): Path =
        Files.copy(ForTestCompileRuntime.runtimeJarForTests().toPath(), tempDir.resolve("stdlib.jar"))

    @Test
    fun jarRoot() {
        val jar = stdlibCopy()
        assertSameParts(jar) {
            assertTrue(JarFilePool.isOpenInAnyPool(jar), "the module mapping is read through the jar pool")
        }
    }

    @Test
    fun directoryRoot() {
        val directory = tempDir.resolve("classes")
        val metaInf = directory.resolve("META-INF").createDirectories()
        ZipFile(stdlibCopy().toFile()).use { zip ->
            for (entry in zip.entries()) {
                if (!entry.isDirectory && entry.name.startsWith("META-INF/") && entry.name.endsWith(".kotlin_module")) {
                    zip.getInputStream(entry).use { Files.copy(it, metaInf.resolve(entry.name.substringAfterLast('/'))) }
                }
            }
        }
        assertSameParts(directory)
    }
}
