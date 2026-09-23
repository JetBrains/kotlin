/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.internal.NativeImageCompilerSession
import org.jetbrains.kotlin.buildtools.internal.nativeImageCompilerCommand
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.*

@Timeout(60)
class NativeImageCompilerSessionTest {
    @TempDir
    lateinit var directory: Path

    private fun startCompiler(): Process {
        val nativeHome = System.getProperty("kotlin.test.native-image.home")
        return if (nativeHome != null) {
            ProcessBuilder(nativeImageCompilerCommand(nativeHome, null)).redirectError(ProcessBuilder.Redirect.INHERIT).start()
        } else {
            startJava("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler", "--native-image-server")
        }
    }

    private fun startJava(main: String, vararg arguments: String): Process {
        val classpath = generateSequence(javaClass.classLoader) { it.parent }
            .filterIsInstance<URLClassLoader>()
            .flatMap { it.urLs.asSequence() }
            .map { File(it.toURI()).absolutePath }
            .toList() + System.getProperty("java.class.path").split(File.pathSeparator)
        return ProcessBuilder(
            listOf(
                File(System.getProperty("java.home"), "bin/java").absolutePath,
                "-cp", classpath.distinct().joinToString(File.pathSeparator), main,
            ) + arguments
        ).redirectError(ProcessBuilder.Redirect.INHERIT).start()
    }

    private fun NativeImageCompilerSession.compile(vararg arguments: String, onCancel: (() -> Unit) -> Unit = {}): Int = compile(
        arguments.toList(), directory.resolve("stdout"), directory.resolve("stderr"), {}, onCancel,
    )

    @Test
    fun reusesWorkerAfterCompilationErrorsAndDoesNotLeakClasspath() {
        val processes = mutableListOf<Process>()
        val session = NativeImageCompilerSession { startCompiler().also { processes.add(it) } }
        session.use {
            val first = directory.resolve("First.kt").toFile().apply { writeText("class First") }
            val second = directory.resolve("Second.kt").toFile().apply { writeText("class Second(val first: First)") }
            val firstOutput = directory.resolve("first").toString()
            val secondOutput = directory.resolve("second").toString()
            val stdlib = File(Unit::class.java.protectionDomain.codeSource.location.toURI()).absolutePath
            fun compile(expected: Int, source: File, output: String, classpath: String = stdlib) {
                val result = session.compile("-no-stdlib", "-no-reflect", source.path, "-classpath", classpath, "-d", output)
                assertEquals(expected, result, directory.resolve("stderr").toFile().readText())
            }
            compile(0, first, firstOutput)
            compile(0, second, secondOutput, "$stdlib${File.pathSeparator}$firstOutput")
            compile(1, second, secondOutput)
            assertTrue(directory.resolve("stderr").toFile().readText().contains("First"))
            compile(0, first, firstOutput)
            assertEquals(1, processes.size)
            assertTrue(processes.single().isAlive)
        }
        assertTrue(processes.single().waitFor(10, TimeUnit.SECONDS))
        assertEquals(0, processes.single().exitValue())
        assertFailsWith<IllegalStateException> { session.compile("-version") }
    }

    @Test
    fun cancellationKillsBlockedWorkerAndNextRequestStartsAnother() {
        val started = CountDownLatch(1)
        val cancel = AtomicReference<() -> Unit>()
        val failure = AtomicReference<Throwable>()
        val processes = mutableListOf<Process>()
        NativeImageCompilerSession {
            (if (processes.isEmpty()) startJava(BlockedWorker::class.java.name) else startCompiler())
                .also { processes.add(it); started.countDown() }
        }.use { session ->
            val caller = thread {
                try {
                    session.compile("-version", onCancel = { cancel.set(it) })
                } catch (e: Throwable) {
                    failure.set(e)
                }
            }
            try {
                assertTrue(started.await(10, TimeUnit.SECONDS))
                cancel.get().invoke()
                caller.join(10_000)
                assertFalse(caller.isAlive)
                assertIs<IOException>(failure.get())
                assertEquals(0, session.compile("-version"))
                // Cancellation from a completed request must not terminate the replacement worker.
                cancel.get().invoke()
                assertEquals(0, session.compile("-version"))
                assertEquals(2, processes.size)
            } finally {
                session.close()
                caller.join(10_000)
            }
        }
        processes.forEach { assertTrue(it.waitFor(10, TimeUnit.SECONDS)) }
    }

    @Test
    fun closeWithoutRequestsDoesNotStartWorker() {
        NativeImageCompilerSession { error("Should not start a process") }.close()
    }

    @Test
    fun distributionLookupIsPortableAndPropertyTakesPrecedence() {
        val home = directory.resolve("distribution with spaces").toFile()
        val suffix = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) ".exe" else ""
        val executable = home.resolve("bin/kotlinc-native-image$suffix")
        executable.parentFile.mkdirs()
        executable.writeText("")
        assertTrue(executable.setExecutable(true))
        val command = nativeImageCompilerCommand(home.path, "not-used")
        assertEquals(executable.absolutePath, command.first())
        assertContains(command, "-Dkotlin.home=${home.absolutePath}")
        assertEquals("--native-image-server", command.last())
        assertEquals(command, nativeImageCompilerCommand(null, home.path))
        assertFailsWith<IOException> { nativeImageCompilerCommand(null, null) }
        assertFailsWith<IOException> { nativeImageCompilerCommand("does-not-exist", home.path) }
    }

    object BlockedWorker {
        @JvmStatic
        fun main(args: Array<String>) {
            Thread.sleep(Long.MAX_VALUE)
        }
    }
}