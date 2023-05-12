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

package org.jetbrains.kotlin.jvm.repl

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.GenericReplCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.KOTLIN_REPL_JVM_TARGET_PROPERTY
import org.jetbrains.kotlin.scripting.definitions.StandardScriptDefinition
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.junit.Assert
import java.io.File

private const val library = "inline fun<T> foo(fn: () -> T): T = fn()"
private const val script = "import foo\nval x = foo { 0 }"

class ReplCompilerJava8Test : KtUsefulTestCase() {
    private lateinit var tmpdir: File

    override fun setUp() {
        super.setUp()
        tmpdir = KotlinTestUtils.tmpDirForTest(this)

        File(tmpdir, "library.kt").writeText(library)

        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.FULL_JDK).apply {
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false))
            addKotlinSourceRoot(tmpdir.absolutePath)
            put(JVMConfigurationKeys.OUTPUT_DIRECTORY, tmpdir)
            loadScriptingPlugin(this)
        }

        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        val res = KotlinToJVMBytecodeCompiler.compileBunchOfSources(environment)
        Assert.assertTrue(res)
    }

    fun testIncompatibleScriptJvmTargetConfig() {
        val configuration = makeConfiguration().apply {
            put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_6)
        }

        val result = runTest(configuration)
        Assert.assertTrue(result is ReplCompileResult.Error)
        Assert.assertTrue((result as ReplCompileResult.Error).message.contains("error: cannot inline bytecode built with JVM target 1.8 into bytecode that is being built with JVM target 1.6"))
    }

    fun testIncompatibleScriptJvmTargetProperty() {
        val configuration = makeConfiguration()
        System.setProperty(KOTLIN_REPL_JVM_TARGET_PROPERTY, "1.6")
        try {
            val result = runTest(configuration)
            Assert.assertTrue(result is ReplCompileResult.Error)
            Assert.assertTrue((result as ReplCompileResult.Error).message.contains("error: cannot inline bytecode built with JVM target 1.8 into bytecode that is being built with JVM target 1.6"))
        }
        finally {
            System.clearProperty(KOTLIN_REPL_JVM_TARGET_PROPERTY)
        }
    }

    fun testCompatibleScriptJvmTargetJavaVersionDetect() {
        val configuration = makeConfiguration()
        val result = runTest(configuration)
        Assert.assertTrue(result is ReplCompileResult.CompiledClasses)
    }

    fun testCompatibleScriptJvmTargetProperty() {
        val configuration = makeConfiguration()
        System.setProperty(KOTLIN_REPL_JVM_TARGET_PROPERTY, "1.8")
        try {
            Assert.assertTrue(runTest(configuration) is ReplCompileResult.CompiledClasses)
        }
        finally {
            System.clearProperty(KOTLIN_REPL_JVM_TARGET_PROPERTY)
        }
    }

    private fun makeConfiguration() = KotlinTestUtils.newConfiguration(
        ConfigurationKind.ALL, TestJdkKind.FULL_JDK, File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-stdlib.jar"), tmpdir
    ).also {
        loadScriptingPlugin(it)
    }

    private fun runTest(configuration: CompilerConfiguration): ReplCompileResult {
        val collector = PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false)
        val replCompiler = GenericReplCompiler(testRootDisposable,
                                               StandardScriptDefinition, configuration, collector)
        val state = replCompiler.createState()

        return replCompiler.compile(state, ReplCodeLine(0, 0, script))
    }
}
