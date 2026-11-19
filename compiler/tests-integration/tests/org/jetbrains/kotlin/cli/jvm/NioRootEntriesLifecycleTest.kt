/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.common.testEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.CliVirtualFileFinderFactory
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtension
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtensions
import org.jetbrains.kotlin.cli.jvm.index.nio.JarClassFileHandle
import org.jetbrains.kotlin.cli.jvm.index.nio.JarFilePool
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.useNioJvmDependenciesIndex
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.disposeRootDisposable
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.outputStream

/** The archives of NIO root entries are released when the compiler environment owning them is disposed. */
class NioRootEntriesLifecycleTest {
    @TempDir
    lateinit var tempDir: Path

    private fun jarWithClass(): Path = tempDir.resolve("lib.jar").also { jar ->
        ZipOutputStream(jar.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("p/A.class"))
            out.write(byteArrayOf(1, 2, 3))
            out.closeEntry()
        }
    }

    private fun configuration(jar: Path): CompilerConfiguration = KotlinTestUtils.newConfiguration().apply {
        put(JVMConfigurationKeys.NO_JDK, true)
        useNioJvmDependenciesIndex = true
        addJvmClasspathRoot(jar.toFile())
    }

    /** Reads `p/A` through the class finder of [project] and checks that the archive stays open until [dispose]. */
    private fun checkReleasedOnDispose(jar: Path, project: Project, dispose: () -> Unit) {
        val index = (project.getService(VirtualFileFinderFactory::class.java) as CliVirtualFileFinderFactory).index
        val handle = index.findClassFiles(ClassId.fromString("p/A"), JavaFileExtensions(JavaFileExtension.CLASS)).single()
        assertInstanceOf(JarClassFileHandle::class.java, handle)
        assertArrayEquals(byteArrayOf(1, 2, 3), handle.readBytes())
        assertTrue(JarFilePool.isOpenInAnyPool(jar), "the archive is mapped while the environment lives")

        dispose()
        assertFalse(JarFilePool.isOpenInAnyPool(jar), "disposing the environment releases the archive")
        Files.delete(jar)
    }

    @Test
    fun kotlinCoreEnvironmentReleasesArchivesOnDispose() {
        val jar = jarWithClass()
        val disposable = Disposer.newDisposable()
        @OptIn(CoreEnvironmentDeprecation::class)
        val environment =
            KotlinCoreEnvironment.create(disposable, configuration(jar))
        checkReleasedOnDispose(jar, environment.project) { disposeRootDisposable(disposable) }
    }

    @Test
    fun pipelineProjectEnvironmentReleasesArchivesOnDispose() {
        val jar = jarWithClass()
        val disposable = Disposer.newDisposable()
        val configuration = configuration(jar).apply { testEnvironment = true }
        val projectEnvironment =
            JvmFrontendPipelinePhase.createProjectEnvironment(configuration, disposable)
        checkReleasedOnDispose(jar, projectEnvironment.project) { disposeRootDisposable(disposable) }
    }
}
