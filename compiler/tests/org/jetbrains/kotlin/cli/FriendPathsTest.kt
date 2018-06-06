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

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.junit.Assert
import java.io.File

/**
 * This test checks that [org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments.friendPaths] works by invoking the compiler's
 * "exec" method directly. Once there's a CLI argument for friend paths, it can be simplified to use that CLI argument instead.
 */
class FriendPathsTest : TestCaseWithTmpdir() {
    private fun getTestDataDirectory(): File = File("compiler/testData/friendPaths/")

    fun testArchive() {
        val libSrc = File(getTestDataDirectory(), "lib.kt")
        val libDest = File(tmpdir, "lib.jar")
        CompilerTestUtil.executeCompilerAssertSuccessful(K2JVMCompiler(), listOf("-d", libDest.path, libSrc.path))

        Assert.assertEquals(ExitCode.OK, invokeCompiler(libDest.path))
    }

    fun testDirectory() {
        val libSrc = File(getTestDataDirectory(), "lib.kt")
        val libDest = File(tmpdir, "lib")
        CompilerTestUtil.executeCompilerAssertSuccessful(K2JVMCompiler(), listOf("-d", libDest.path, libSrc.path))

        Assert.assertEquals(ExitCode.OK, invokeCompiler(libDest.path))
    }

    private fun invokeCompiler(libraryPath: String): ExitCode {
        return K2JVMCompiler().exec(ThrowingMessageCollector(), Services.EMPTY, K2JVMCompilerArguments().apply {
            destination = tmpdir.path
            classpath = libraryPath
            freeArgs = arrayListOf(File(getTestDataDirectory(), "usage.kt").path)

            friendPaths = arrayOf(libraryPath)
        })
    }

    private class ThrowingMessageCollector : MessageCollector {
        override fun clear() {}

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
            if (severity.isError) {
                Assert.fail("${severity.presentableName}: $message at $location")
            }
        }

        override fun hasErrors(): Boolean = false
    }
}
