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
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import java.io.File

class FriendPathsTest : TestCaseWithTmpdir() {
    private fun getTestDataDirectory(): File = File("compiler/testData/friendPaths/")

    fun testArchive() {
        val libSrc = File(getTestDataDirectory(), "lib.kt")
        val libDest = File(tmpdir, "lib.jar")
        CompilerTestUtil.executeCompilerAssertSuccessful(K2JVMCompiler(), listOf("-d", libDest.path, libSrc.path))

        CompilerTestUtil.executeCompilerAssertSuccessful(
            K2JVMCompiler(),
            listOf(
                "-d", tmpdir.path, "-cp", libDest.path, File(getTestDataDirectory(), "usage.kt").path,
                "-Xfriend-paths=${libDest.path}"
            )
        )
    }

    fun testDirectory() {
        val libSrc = File(getTestDataDirectory(), "lib.kt")
        val libDest = File(tmpdir, "lib")
        CompilerTestUtil.executeCompilerAssertSuccessful(K2JVMCompiler(), listOf("-d", libDest.path, libSrc.path))

        CompilerTestUtil.executeCompilerAssertSuccessful(
            K2JVMCompiler(),
            listOf(
                "-d", tmpdir.path, "-cp", libDest.path, File(getTestDataDirectory(), "usage.kt").path,
                "-Xfriend-paths=${libDest.path}"
            )
        )
    }
}
