/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.integration

import com.intellij.openapi.util.SystemInfo
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.fileUtils.descendantRelativeTo
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
        fun compileWithClasspath(source: String, fileName: String, outputPath: String, classpath: File? = null): File {
            val programSource = File(tmpdir, fileName)
            programSource.writeText(source)

            val output = File(tmpdir, outputPath)
            val (stdout, exitCode) = AbstractCliTest.executeCompilerGrabOutput(
                K2JVMCompiler(),
                buildList {
                    add(programSource.path)
                    add("-d")
                    add(output.path)
                    add("-cp")
                    add(PathUtil.kotlinPathsForDistDirectory.compilerPath.absolutePath)
                    if (classpath != null) {
                        add("-cp")
                        add(classpath.path)
                    }
                }
            )
            assertEquals("Compilation failed:\n$stdout", ExitCode.OK, exitCode)
            return output
        }

        // Create a symlink, if possible, and return path to it. Otherwise, return input file path.
        fun createSymlink(from: File, to: File): File = try {
            to.toPath().createSymbolicLinkPointingTo(from.toPath())
            to
        } catch (e: Throwable) {
            from
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

        // There are two possible scenarios - when the output is a dir and when it is a jar

        run {
            val libOut = compileWithClasspath(
                source = """fun foo():String = "OK" """,
                fileName = "lib.kt",
                outputPath = "lib"
            )
            val link = createSymlink(libOut, File(tmpdir, "liblink"))
            val outDir = compileWithClasspath(
                source = "fun main() { println(foo()) }",
                fileName = "main.kt",
                outputPath = "out",
                classpath = link
            )
            run(outDir, link)
        }

        run {
            val libOut = compileWithClasspath(
                source = """fun foo():String = "OK" """,
                fileName = "lib.kt",
                outputPath = "lib.jar"
            )
            val link = createSymlink(libOut, File(tmpdir, "liblink.jar"))
            val outDir = compileWithClasspath(
                source = "fun main() { println(foo()) }",
                fileName = "main.kt",
                outputPath = "out",
                classpath = link
            )
            run(outDir, link)
        }
    }
}
