/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.build

import com.google.common.io.Files
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.IncrementalCacheImpl
import org.jetbrains.kotlin.incremental.compileChanged
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

public class SimpleBuildTest : KotlinIntegrationTestBase() {

    class TestMessageCollector() : MessageCollector {
        data class Record(val severity: CompilerMessageSeverity, val message: String, val location: CompilerMessageLocation)
        val records = arrayListOf<Record>()
        public override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
            records.add(Record(severity, message, location))
        }
    }

    private fun getTestBaseDir(): String = KotlinTestUtils.getTestDataPathBase() + "/integration/smoke/" + getTestName(true)
    private fun getHelloAppBaseDir(): String = KotlinTestUtils.getTestDataPathBase() + "/integration/smoke/helloApp"

    private fun run(logName: String, vararg args: String): Int = runJava(getTestBaseDir(), logName, *args)

    public fun testHelloApp() {

        val kotlinPaths = PathUtil.getKotlinPathsForDistDirectory()
        val moduleName = "abc"
        val targetType = "java-production"
        val outputPath = File(tmpdir, "hello.jar")
        val args = K2JVMCompilerArguments()
        args.includeRuntime = true
        val sources = listOf(File(getTestBaseDir(), "hello.kt"))
        val messageCollector = TestMessageCollector()

        Files.createTempDir()

        compileChanged<TargetId>(
                kotlinPaths,
                moduleName = moduleName,
                isTest = false,
                targets = listOf(TargetId(moduleName, targetType)),
                getDependencies = { listOf<TargetId>() },
                commonArguments = args,
                k2JvmArguments = args,
                additionalArguments = listOf(),
                outputDir = outputPath,
                sourcesToCompile = sources,
                javaSourceRoots = sources.filter { it.isDirectory },
                classpath = listOf(),
                friendDirs = listOf(),
                compilationCanceledStatus = object : CompilationCanceledStatus {
                    override fun checkCanceled() {
                    }
                },
                getIncrementalCache = { IncrementalCacheImpl(outputPath, Files.createTempDir(), it) },
                getTargetId = { this },
                messageCollector = messageCollector
        )

//        assertEquals(listOf<String>(), messageCollector.records.map { it.message })
        assertTrue(outputPath.exists())
        run("hello.run", "-cp", "${outputPath.absolutePath}:${kotlinPaths.runtimePath.absolutePath}", "Hello.HelloKt")
    }
}
