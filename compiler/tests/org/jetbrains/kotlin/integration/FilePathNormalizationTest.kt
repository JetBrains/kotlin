/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.integration

import com.intellij.openapi.util.SystemInfo
import org.jetbrains.kotlin.utils.fileUtils.descendantRelativeTo
import java.io.File
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
}
