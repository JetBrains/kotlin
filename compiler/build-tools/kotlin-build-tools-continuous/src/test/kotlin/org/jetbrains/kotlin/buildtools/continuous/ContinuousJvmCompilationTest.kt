/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.continuous

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

@DisplayName("Continuous compilation with the error tolerance plugin")
class ContinuousJvmCompilationTest {
    @TempDir
    lateinit var workingDir: Path

    private val sourceRoot: Path get() = workingDir.resolve("src")
    private val destination: Path get() = workingDir.resolve("classes")

    @Test
    @DisplayName("Submitted changes with errors are compiled into throws, and fixed on the next change")
    fun testSubmittedChanges() {
        val a = writeSource("a.kt", "fun a(): Int = 1")
        val b = writeSource("b.kt", "fun b(): Int = a() + 1")
        withContinuousCompilation(listOf(a, b)) { compilation, rounds ->
            compilation.start()
            assertTrue(compilation.awaitIdle(TIMEOUT))
            assertEquals(CompilationResult.COMPILATION_SUCCESS, rounds.last().result)
            assertEquals(2, callTopLevelFunction("BKt", "b"))

            b.writeText("fun b(): Int = newA() + 1")
            compilation.submitChanges(listOf(b))
            assertTrue(compilation.awaitIdle(TIMEOUT))
            assertEquals(CompilationResult.COMPILATION_SUCCESS, rounds.last().result)
            val error = assertThrows<Error> { callTopLevelFunction("BKt", "b") }
            assertTrue("Unresolved reference 'newA'" in error.message.orEmpty(), error.message)

            a.writeText("fun a(): Int = 1\nfun newA(): Int = 41")
            compilation.submitChanges(listOf(a))
            assertTrue(compilation.awaitIdle(TIMEOUT))
            assertEquals(CompilationResult.COMPILATION_SUCCESS, rounds.last().result)
            assertEquals(42, callTopLevelFunction("BKt", "b"))
        }
    }

    @Test
    @DisplayName("Changes on disk are detected by the watcher, including new and removed files")
    fun testWatcher() {
        val a = writeSource("a.kt", "fun a(): Int = 1")
        withContinuousCompilation(listOf(a)) { compilation, rounds ->
            compilation.start()
            assertTrue(compilation.awaitIdle(TIMEOUT))
            compilation.watch(listOf(sourceRoot), pollIntervalMillis = 50)

            val c = writeSource("c.kt", "fun c(): Int = a() + 2")
            awaitRounds(rounds, count = 2)
            assertTrue(compilation.awaitIdle(TIMEOUT))
            assertEquals(CompilationResult.COMPILATION_SUCCESS, rounds.last().result)
            assertEquals(3, callTopLevelFunction("CKt", "c"))

            c.toFile().delete()
            awaitRounds(rounds, count = 3)
            assertTrue(compilation.awaitIdle(TIMEOUT))
            val changes = rounds.last().sourcesChanges as SourcesChanges.Known
            assertEquals(listOf(c.toFile().absoluteFile), changes.removedFiles.map { it.absoluteFile })
            assertTrue(!destination.resolve("CKt.class").toFile().exists())
        }
    }

    @OptIn(DelicateBuildToolsApi::class)
    private fun withContinuousCompilation(
        sources: List<Path>,
        block: (ContinuousJvmCompilation, List<ContinuousJvmCompilation.Round>) -> Unit,
    ) {
        val toolchains = KotlinToolchains.loadImplementation(implementationClasspath)
        val rounds = CopyOnWriteArrayList<ContinuousJvmCompilation.Round>()
        toolchains.createBuildSession().use { session ->
            val compilation = ContinuousJvmCompilation(
                session,
                toolchains.createInProcessExecutionPolicy(),
                sources,
                createOperation = { currentSources, changes ->
                    toolchains.jvm.jvmCompilationOperationBuilder(currentSources, destination).apply {
                        compilerArguments.applyCommandLineArguments(
                            listOf(
                                "-no-stdlib", "-no-reflect",
                                "-classpath", stdlib.toString(),
                                "-module-name", "test",
                                "-Xplugin=$errorTolerancePlugin",
                            )
                        )
                        this[JvmCompilationOperation.INCREMENTAL_COMPILATION] = snapshotBasedIcConfigurationBuilder(
                            workingDir.resolve("ic"), changes, dependenciesSnapshotFiles = emptyList()
                        ).build()
                    }.build()
                },
                listener = { rounds.add(it) },
                debounceMillis = 10,
            )
            compilation.use { block(it, rounds) }
        }
    }

    private fun writeSource(name: String, text: String): Path =
        sourceRoot.createDirectories().resolve(name).apply { writeText(text) }

    private fun awaitRounds(rounds: List<ContinuousJvmCompilation.Round>, count: Int) {
        val deadline = System.currentTimeMillis() + TIMEOUT
        while (rounds.size < count) {
            check(System.currentTimeMillis() < deadline) { "Expected $count compilation rounds, but there were ${rounds.size}" }
            Thread.sleep(10)
        }
    }

    private fun callTopLevelFunction(className: String, functionName: String): Int =
        URLClassLoader(arrayOf(destination.toUri().toURL(), stdlib.toUri().toURL()), null).use { classLoader ->
            val function = MethodHandles.publicLookup()
                .findStatic(classLoader.loadClass(className), functionName, MethodType.methodType(Int::class.javaPrimitiveType))
            function.invokeWithArguments() as Int
        }

    companion object {
        private const val TIMEOUT = 120_000L

        private fun classpathFromProperty(name: String): List<Path> =
            System.getProperty(name)!!.split(File.pathSeparator).map { Paths.get(it) }

        private val implementationClasspath = classpathFromProperty("kotlin.build-tools.continuous.test.implementationClasspath")
        private val errorTolerancePlugin = classpathFromProperty("kotlin.build-tools.continuous.test.errorTolerancePlugin").single()
        private val stdlib = implementationClasspath.single { it.fileName.toString().matches(Regex("kotlin-stdlib-[0-9].*\\.jar")) }
    }
}
