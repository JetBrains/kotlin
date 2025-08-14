/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.integration

import com.intellij.openapi.util.SystemInfo
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.fileUtils.descendantRelativeTo
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.io.path.createSymbolicLinkPointingTo

class FilePathNormalizationTest : KotlinIntegrationTestBase() {
    // This test checks that path normalization logic used in MessageRenderer.PLAIN_RELATIVE_PATHS works correctly.
    // It compiles and runs a program in a separate process to be able to test how it works from different working directories.
    // (It could be tested in the same process by changing the user.dir manually, but that could change behavior
    // of other tests run in parallel.)
    fun test() {
        val descendantRelativeTo = File::descendantRelativeTo.name
        val program = ProgramWithDependencyOnCompiler(
            tmpdir, """
            import org.jetbrains.kotlin.utils.fileUtils.$descendantRelativeTo
            import java.io.File

            fun main(args: Array<String>) {
                println(File(args[0]).$descendantRelativeTo(File(".").absoluteFile).path)
            }
            """.trimIndent()
        )

        program.compile()

        fun doTest(cwd: File, filePath: String, expectedWithForwardSlash: String) {
            // We use "/" below for simplicity, but the actual paths in compiler messages use the system separator.
            val expected = expectedWithForwardSlash.replace("/", File.separator)
            val actual = program.run(cwd, filePath)
            assertEquals("cwd: $cwd\nfilePath: $filePath\n", expected, actual)
        }

        doTest(tmpdir, "a", "a")
        doTest(tmpdir, "./a", "a")
        doTest(tmpdir, "a/../b", "b")
        doTest(tmpdir, "..", "..")
        doTest(tmpdir, "../a", "../a")
        doTest(tmpdir, tmpdir.path + "/a", "a")
        doTest(tmpdir, tmpdir.path + "/./a", "a")

        val root = File("/")
        doTest(root, "test", "test")

        doTest(root, tmpdir.path + "/a", (tmpdir.path + "/a").removePrefix(root.absolutePath))

        doTest(
            root, "./test",
            if (SystemInfo.isWindows) "./test" else "test"
        )

        // Check symbolic links, but skip file systems which don't support them (e.g. Windows).
        fun doSymbolicLinkTest(cwd: File, source: File, target: File, expected: String) {
            val link = try {
                source.toPath().createSymbolicLinkPointingTo(target.toPath()).toFile()
            } catch (e: Throwable) {
                null
            }
            if (link != null) {
                doTest(cwd, link.path, expected)
            }
        }

        doSymbolicLinkTest(tmpdir, tmpdir / "a", tmpdir / "b", "a")
        doSymbolicLinkTest(tmpdir / "unrelated", tmpdir / "a", tmpdir / "../b", tmpdir.path + "/a")
    }

    private operator fun File.div(x: String): File = File(this, x)

    // Check, that the compiler still works, when classpath entry is passed by symlink
    fun testSymlinkInClasspath() {
        fun compileWithClasspath(source: String, fileName: String, outputFile: File, classpath: File? = null) {
            val programSource = File(tmpdir, fileName)
            programSource.writeText(source)

            val (stdout, exitCode) = AbstractCliTest.executeCompilerGrabOutput(
                K2JVMCompiler(),
                buildList {
                    this += programSource.path
                    this += "-d"
                    this += outputFile.path
                    this += "-cp"
                    this += listOfNotNull(
                        PathUtil.kotlinPathsForDistDirectory.compilerPath.absolutePath,
                        classpath?.path
                    ).joinToString(File.pathSeparator)
                }
            )
            assertEquals("Compilation failed:\n$stdout", ExitCode.OK, exitCode)
        }

        // Run the compiled file
        fun run(outDir: File, classpath: File) {
            val stdout = ProgramWithDependencyOnCompiler.runJava(
                tmpdir,
                "-cp",
                listOf(
                    PathUtil.kotlinPathsForDistDirectory.compilerPath.absolutePath,
                    outDir.path,
                    classpath.path
                ).joinToString(File.pathSeparator),
                "MainKt"
            )
            assertEquals("OK", stdout)
        }

        // There are four possible scenarios:
        // (the output is a dir | the output is a jar) x (symlink is the last path segment | symlink is not the last path segment)

        // The output is a dir:
        run {
            val baseDir = tmpdir.resolve("base").apply { mkdirs() }
            val libDir = baseDir.resolve("lib")
            compileWithClasspath(
                source = """fun foo():String = "OK" """,
                fileName = "lib.kt",
                outputFile = libDir
            )

            // The symlink is the last path segment:
            run {
                val linkToLibDir = createSymlink(libDir, File(tmpdir, "liblink"))
                val outDir = tmpdir.resolve("out")
                compileWithClasspath(
                    source = "fun main() { println(foo()) }",
                    fileName = "main.kt",
                    outputFile = outDir,
                    classpath = linkToLibDir
                )
                run(outDir, linkToLibDir)
            }

            // The symlink is not the last path segment:
            run {
                val linkToBaseDir = createSymlink(baseDir, File(tmpdir, "baselink"))
                val libDirWithLink = linkToBaseDir.resolve("lib")
                val outDir = tmpdir.resolve("out")
                compileWithClasspath(
                    source = "fun main() { println(foo()) }",
                    fileName = "main.kt",
                    outputFile = outDir,
                    classpath = libDirWithLink
                )
                run(outDir, libDirWithLink)
            }
        }

        tmpdir.deleteRecursively()
        tmpdir.mkdirs()

        // The output is a jar:
        run {
            val baseDir = tmpdir.resolve("base").apply { mkdirs() }
            val libJar = baseDir.resolve("lib.jar")
            compileWithClasspath(
                source = """fun foo():String = "OK" """,
                fileName = "lib.kt",
                outputFile = libJar
            )

            // The symlink is the last path segment:
            run {
                val linkToLibJar = createSymlink(libJar, File(tmpdir, "liblink.jar"))
                val outDir = tmpdir.resolve("out")
                compileWithClasspath(
                    source = "fun main() { println(foo()) }",
                    fileName = "main.kt",
                    outputFile = outDir,
                    classpath = linkToLibJar
                )
                run(outDir, linkToLibJar)
            }

            // The symlink is the last path segment and without extension:
            run {
                val linkToLibJar = createSymlink(libJar, File(tmpdir, "liblink"))
                val outDir = tmpdir.resolve("out")
                compileWithClasspath(
                    source = "fun main() { println(foo()) }",
                    fileName = "main.kt",
                    outputFile = outDir,
                    classpath = linkToLibJar
                )
                run(outDir, linkToLibJar)
            }

            // The symlink is not the last path segment:
            run {
                val linkToBaseDir = createSymlink(baseDir, File(tmpdir, "baselink"))
                val libJarWithLink = linkToBaseDir.resolve("lib.jar")
                val outDir = tmpdir.resolve("out")
                compileWithClasspath(
                    source = "fun main() { println(foo()) }",
                    fileName = "main.kt",
                    outputFile = outDir,
                    classpath = libJarWithLink
                )
                run(outDir, libJarWithLink)
            }
        }
    }

    fun testSymlinkInJsKlibPath() {
        fun compileKlib(source: String, fileName: String, outputFile: File, dependency: File? = null) {
            val programSource = File(tmpdir, fileName)
            programSource.writeText(source)

            val libraries = listOfNotNull(
                StandardLibrariesPathProviderForKotlinProject.fullJsStdlib(),
                dependency,
            ).joinToString(File.pathSeparator) { it.path }

            val args = buildList {
                if (outputFile.extension == "klib") {
                    this += K2JSCompilerArguments::irProduceKlibFile.cliArgument
                    this += K2JSCompilerArguments::outputDir.cliArgument
                    this += outputFile.parentFile.path
                } else {
                    this += K2JSCompilerArguments::irProduceKlibDir.cliArgument
                    this += K2JSCompilerArguments::outputDir.cliArgument
                    this += outputFile.path
                }
                this += K2JSCompilerArguments::libraries.cliArgument
                this += libraries
                this += K2JSCompilerArguments::moduleName.cliArgument
                this += outputFile.nameWithoutExtension
                this += programSource.path
            }.toTypedArray()

            val compilerXmlOutput = ByteArrayOutputStream()
            val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
                K2JSCompiler().execFullPathsInMessages(printStream, args)
            }

            assertEquals("Compilation failed:\n$compilerXmlOutput", ExitCode.OK, exitCode)
        }

        // There are four possible scenarios:
        // (the output is a dir | the output is a klib) x (symlink is the last path segment | symlink is not the last path segment)

        // The output is a dir:
        run {
            val baseDir = tmpdir.resolve("base").apply { mkdirs() }
            val libDir = baseDir.resolve("lib")
            compileKlib(
                source = """fun foo():String = "OK" """,
                fileName = "lib.kt",
                outputFile = libDir
            )

            // The symlink is the last path segment:
            run {
                val linkToLibDir = createSymlink(libDir, File(tmpdir, "liblink"))
                val outDir = tmpdir.resolve("out")
                compileKlib(
                    source = "fun main() { println(foo()) }",
                    fileName = "main.kt",
                    outputFile = outDir,
                    dependency = linkToLibDir
                )
            }

            // The symlink is not the last path segment:
            run {
                val linkToBaseDir = createSymlink(baseDir, File(tmpdir, "baselink"))
                val libDirWithLink = linkToBaseDir.resolve("lib")
                val outDir = tmpdir.resolve("out")
                compileKlib(
                    source = "fun main() { println(foo()) }",
                    fileName = "main.kt",
                    outputFile = outDir,
                    dependency = libDirWithLink
                )
            }
        }

        tmpdir.deleteRecursively()
        tmpdir.mkdirs()

        // The output is a klib:
        run {
            val baseDir = tmpdir.resolve("base").apply { mkdirs() }
            val libKlib = baseDir.resolve("lib.klib")
            compileKlib(
                source = """fun foo():String = "OK" """,
                fileName = "lib.kt",
                outputFile = libKlib
            )

            // The symlink is the last path segment:
            run {
                val linkToLibKlib = createSymlink(libKlib, File(tmpdir, "liblink.klib"))
                val outDir = tmpdir.resolve("out")
                compileKlib(
                    source = "fun main() { println(foo()) }",
                    fileName = "main.kt",
                    outputFile = outDir,
                    dependency = linkToLibKlib
                )
            }

            // The symlink is the last path segment and without extension:
            run {
                val linkToLibKlib = createSymlink(libKlib, File(tmpdir, "liblink"))
                val outDir = tmpdir.resolve("out")
                compileKlib(
                    source = "fun main() { println(foo()) }",
                    fileName = "main.kt",
                    outputFile = outDir,
                    dependency = linkToLibKlib
                )
            }

            // The symlink is not the last path segment:
            run {
                val linkToBaseDir = createSymlink(baseDir, File(tmpdir, "baselink"))
                val libKlibWithLink = linkToBaseDir.resolve("lib.klib")
                val outDir = tmpdir.resolve("out")
                compileKlib(
                    source = "fun main() { println(foo()) }",
                    fileName = "main.kt",
                    outputFile = outDir,
                    dependency = libKlibWithLink
                )
            }
        }
    }

    // Create a symlink, if possible, and return path to it. Otherwise, return input file path.
    private fun createSymlink(from: File, to: File): File = try {
        to.toPath().createSymbolicLinkPointingTo(from.toPath())
        to
    } catch (_: Throwable) {
        from
    }
}
