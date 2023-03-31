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

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.incremental.utils.TestCompilationResult
import org.jetbrains.kotlin.incremental.utils.TestICReporter
import org.jetbrains.kotlin.incremental.utils.TestMessageCollector
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.ByteArrayOutputStream
import java.io.File
import javax.tools.ToolProvider

abstract class AbstractIncrementalJvmCompilerRunnerTest : AbstractIncrementalCompilerRunnerTestBase<K2JVMCompilerArguments>() {
    override fun make(cacheDir: File, outDir: File, sourceRoots: Iterable<File>, args: K2JVMCompilerArguments): TestCompilationResult {
        val reporter = TestICReporter()
        val messageCollector = TestMessageCollector()
        makeIncrementally(cacheDir, sourceRoots, args, reporter = reporter, messageCollector = messageCollector)
        val kotlinCompileResult = TestCompilationResult(reporter, messageCollector)
        if (kotlinCompileResult.exitCode != ExitCode.OK) return kotlinCompileResult

        val (javaExitCode, _, javaErrors) = compileJava(sourceRoots, args.destination!!)
        return when (javaExitCode) {
            ExitCode.OK -> kotlinCompileResult
            else -> kotlinCompileResult.copy(exitCode = javaExitCode, compileErrors = javaErrors)
        }
    }

    private fun compileJava(sourceRoots: Iterable<File>, kotlinClassesPath: String): TestCompilationResult {
        val javaSources = arrayListOf<File>()
        for (root in sourceRoots) {
            javaSources.addAll(root.walk().filter { it.isFile && it.extension == "java" })
        }
        if (javaSources.isEmpty()) return TestCompilationResult(ExitCode.OK, emptyList(), emptyList())

        val javaClasspath = compileClasspath + kotlinClassesPath
        val javaDestinationDir = File(workingDir, "java-classes").apply {
            if (exists()) {
                deleteRecursively()
            }
            mkdirs()
        }
        val args = arrayOf("-cp", javaClasspath.joinToString(File.pathSeparator),
                           "-d", javaDestinationDir.canonicalPath,
                           *javaSources.map { it.canonicalPath }.toTypedArray()
        )

        val err = ByteArrayOutputStream()
        val javac = ToolProvider.getSystemJavaCompiler()
        val rc = javac.run(null, null, err, *args)

        val exitCode = if (rc == 0) ExitCode.OK else ExitCode.COMPILATION_ERROR
        val errors = err.toString().split("\n")
        return TestCompilationResult(exitCode, javaSources, errors)
    }

    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JVMCompilerArguments =
        K2JVMCompilerArguments().apply {
            moduleName = testDir.name
            destination = destinationDir.path
            classpath = compileClasspath.toTypedArray()
        }

    private val compileClasspath =
        listOf(
            kotlinStdlibJvm,
            KtTestUtil.getAnnotationsJar()
        ).map { it.canonicalPath }
}
