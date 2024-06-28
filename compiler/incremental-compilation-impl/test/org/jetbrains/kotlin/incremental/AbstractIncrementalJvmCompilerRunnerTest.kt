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

import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.incremental.multiproject.EmptyModulesApiHistory
import org.jetbrains.kotlin.incremental.utils.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.ByteArrayOutputStream
import java.io.File
import javax.tools.ToolProvider

abstract class AbstractIncrementalJvmCompilerRunnerTest : AbstractIncrementalCompilerRunnerTestBase<K2JVMCompilerArguments>() {
    override fun make(cacheDir: File, outDir: File, sourceRoots: Iterable<File>, args: K2JVMCompilerArguments): TestCompilationResult {
        val reporter = TestICReporter()
        val messageCollector = TestMessageCollector()
        val testLookupTracker = TestLookupTracker(lookupsDuringTest)

        makeIncrementallyForTests(
            cacheDir,
            sourceRoots,
            args,
            reporter = reporter,
            messageCollector = messageCollector,
            testLookupTracker = testLookupTracker
        )

        val kotlinCompileResult = TestCompilationResult(reporter, messageCollector)
        if (kotlinCompileResult.exitCode != ExitCode.OK) return kotlinCompileResult

        val (javaExitCode, _, javaErrors) = compileJava(sourceRoots, args.destination!!)
        return when (javaExitCode) {
            ExitCode.OK -> kotlinCompileResult
            else -> kotlinCompileResult.copy(exitCode = javaExitCode, compileErrors = javaErrors)
        }
    }

    // extension of org.jetbrains.kotlin.incremental.CompilerRunnerUtilsKt.makeJvmIncrementally for tests
    private fun makeIncrementallyForTests(
        cachesDir: File,
        sourceRoots: Iterable<File>,
        args: K2JVMCompilerArguments,
        messageCollector: MessageCollector = MessageCollector.NONE,
        reporter: TestICReporter,
        testLookupTracker: TestLookupTracker
    ) {
        val kotlinExtensions = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
        val allExtensions = kotlinExtensions + "java"
        val rootsWalk = sourceRoots.asSequence().flatMap { it.walk() }
        val files = rootsWalk.filter(File::isFile)
        val sourceFiles = files.filter { it.extension.lowercase() in allExtensions }.toList()
        val buildHistoryFile = File(cachesDir, "build-history.bin")
        args.javaSourceRoots = sourceRoots.map { it.absolutePath }.toTypedArray()
        val buildReporter = TestBuildReporter(testICReporter = reporter, buildMetricsReporter = DoNothingBuildMetricsReporter)

        withIncrementalCompilation(args) {
            val k2Mode = args.useK2 || (
                    (args.languageVersion ?: LanguageVersion.LATEST_STABLE.versionString) >= LanguageVersion.KOTLIN_2_0.versionString
                    )

            val compiler =
                if (k2Mode && args.useFirIC && args.useFirLT /* TODO by @Ilya.Chernikov: move LT check into runner */)
                    IncrementalFirJvmCompilerTestRunner(
                        cachesDir,
                        buildReporter,
                        buildHistoryFile,
                        outputDirs = null,
                        EmptyModulesApiHistory,
                        kotlinExtensions,
                        ClasspathChanges.ClasspathSnapshotDisabled,
                        testLookupTracker
                    )
                else
                    IncrementalJvmCompilerTestRunner(
                        cachesDir,
                        buildReporter,
                        // Use precise setting in case of non-Gradle build
                        usePreciseJavaTracking = !k2Mode, // TODO by @Ilya.Chernikov: add fir-based java classes tracker when available and set this to true
                        buildHistoryFile = buildHistoryFile,
                        outputDirs = null,
                        modulesApiHistory = EmptyModulesApiHistory,
                        kotlinSourceFilesExtensions = kotlinExtensions,
                        classpathChanges = ClasspathChanges.ClasspathSnapshotDisabled,
                        testLookupTracker = testLookupTracker
                    )
            //TODO by @Ilya.Chernikov: set properly
            compiler.compile(sourceFiles, args, messageCollector, changedFiles = null)
        }
    }

    private fun compileJava(sourceRoots: Iterable<File>, kotlinClassesPath: String): TestCompilationResult {
        val javaSources = arrayListOf<File>()
        for (root in sourceRoots) {
            javaSources.addAll(root.walk().filter { it.isFile && it.extension == "java" })
        }
        if (javaSources.isEmpty()) return TestCompilationResult(ExitCode.OK, emptyList(), emptyList(), "")

        val javaClasspath = compileClasspath + File.pathSeparator + kotlinClassesPath
        val javaDestinationDir = File(workingDir, "java-classes").apply {
            if (exists()) {
                deleteRecursively()
            }
            mkdirs()
        }
        val args = arrayOf(
            "-cp", javaClasspath,
            "-d", javaDestinationDir.canonicalPath,
            *javaSources.map { it.canonicalPath }.toTypedArray()
        )

        val err = ByteArrayOutputStream()
        val javac = ToolProvider.getSystemJavaCompiler()
        val rc = javac.run(null, null, err, *args)

        val exitCode = if (rc == 0) ExitCode.OK else ExitCode.COMPILATION_ERROR
        val errors = err.toString().split("\n")
        return TestCompilationResult(exitCode, javaSources, errors, "")
    }

    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JVMCompilerArguments =
        K2JVMCompilerArguments().apply {
            moduleName = testDir.name
            destination = destinationDir.path
            classpath = compileClasspath
        }

    private val compileClasspath =
        listOf(
            kotlinStdlibJvm,
            KtTestUtil.getAnnotationsJar()
        ).joinToString(File.pathSeparator) { it.canonicalPath }
}
