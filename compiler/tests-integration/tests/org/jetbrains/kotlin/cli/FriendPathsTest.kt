/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import java.io.File
import kotlin.io.path.createSymbolicLinkPointingTo

class FriendPathsTest : TestCaseWithTmpdir() {
    private fun getTestDataDirectory(): File = File("compiler/testData/friendPaths/")

    private val absoluteArchive by lazy { File(tmpdir, "lib.jar") }
    private val relativeArchive by lazy { absoluteArchive.relativeTo(File("").absoluteFile) }

    private val absoluteDir by lazy { File(tmpdir, "lib") }
    private val relativeDir by lazy { absoluteDir.relativeTo(File("").absoluteFile) }

    fun testArchive() {
        doTestFriendPaths(absoluteArchive, absoluteArchive)
    }

    /** Regression test for KT-29933. */
    fun testArchiveWithRelativePath() {
        doTestFriendPaths(relativeArchive, relativeArchive)
    }

    fun testDirectory() {
        doTestFriendPaths(absoluteDir, absoluteDir)
    }

    /** Regression test for KT-29933. */
    fun testDirectoryWithRelativePath() {
        doTestFriendPaths(relativeDir, relativeDir)
    }

    fun testArchiveWithRelativeFriendPath() {
        doTestFriendPaths(absoluteArchive, relativeArchive)
    }

    fun testArchiveWithRelativeClasspath() {
        doTestFriendPaths(relativeArchive, absoluteArchive)
    }

    fun testArchiveWithRelativeFriendPathThroughSymlink() {
        val dir = File(tmpdir, "dir")
        dir.mkdir()
        val symlink = File(tmpdir, "symlink")
        symlink.toPath().createSymbolicLinkPointingTo(dir.toPath())
        val libDest = File(symlink, "lib.jar")
        val relativeLibDest = libDest.relativeTo(File("").absoluteFile)
        doTestFriendPaths(libDest, relativeLibDest)
    }

    fun testArchiveWithRelativeClasspathThroughSymlink() {
        val dir = File(tmpdir, "dir")
        dir.mkdir()
        val symlink = File(tmpdir, "symlink")
        symlink.toPath().createSymbolicLinkPointingTo(dir.toPath())
        val libDest = File(symlink, "lib.jar")
        val relativeLibDest = libDest.relativeTo(File("").absoluteFile)
        doTestFriendPaths(relativeLibDest, libDest)
    }

    // This is a regression test for KT-70991.
    // It's not a generated CLI test because the issue only reproduced when any path in the classpath is absolute, while CliTestGenerated
    // passes relative paths of libraries to the classpath.
    fun testNonExistingPath() {
        val libSrc = File(getTestDataDirectory(), "lib.kt")
        CompilerTestUtil.executeCompilerAssertSuccessful(
            K2JVMCompiler(),
            listOf(
                libSrc.path,
                "-d", File(tmpdir, "output").path,
                "-Xfriend-paths=non-existing-path",
                "-no-stdlib",
                "-cp", ForTestCompileRuntime.runtimeJarForTests().absolutePath,
            ),
        )
    }

    private fun doTestFriendPaths(libDest: File, friendPath: File) {
        val libSrc = File(getTestDataDirectory(), "lib.kt")
        CompilerTestUtil.executeCompilerAssertSuccessful(K2JVMCompiler(), listOf("-d", libDest.path, libSrc.path))

        CompilerTestUtil.executeCompilerAssertSuccessful(
            K2JVMCompiler(),
            listOf(
                "-d", tmpdir.path, "-cp", libDest.path, File(getTestDataDirectory(), "usage.kt").path,
                "-Xfriend-paths=${friendPath.path}"
            ),
        )
    }
}
