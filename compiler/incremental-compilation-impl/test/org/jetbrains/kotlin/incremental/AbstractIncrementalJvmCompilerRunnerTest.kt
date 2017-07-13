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

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.incremental.utils.TestCompilationResult
import org.jetbrains.kotlin.incremental.utils.TestICReporter
import org.jetbrains.kotlin.incremental.utils.TestMessageCollector
import java.io.File

abstract class AbstractIncrementalJvmCompilerRunnerTest : AbstractIncrementalCompilerRunnerTestBase<K2JVMCompilerArguments>() {
    override fun make(cacheDir: File, sourceRoots: Iterable<File>, args: K2JVMCompilerArguments): TestCompilationResult {
        val reporter = TestICReporter()
        val messageCollector = TestMessageCollector()
        makeIncrementally(cacheDir, sourceRoots, args, reporter = reporter, messageCollector = messageCollector)
        return TestCompilationResult(reporter, messageCollector)
    }

    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JVMCompilerArguments =
            K2JVMCompilerArguments().apply {
                moduleName = testDir.name
                destination = destinationDir.path
                classpathAsList = listOf(File(bootstrapKotlincLib, "kotlin-stdlib.jar"))
            }
}