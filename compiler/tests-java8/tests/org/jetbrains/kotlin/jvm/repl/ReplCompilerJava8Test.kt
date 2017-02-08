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

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.repl.GenericReplCompiler
import org.jetbrains.kotlin.cli.jvm.repl.KOTLIN_REPL_JVM_TARGET_PROPERTY
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.addKotlinSourceRoot
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import org.jetbrains.kotlin.script.StandardScriptDefinition
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.junit.Assert
import org.junit.Test
import java.io.File

private const val library = "inline fun<T> foo(fn: () -> T): T = fn()"
private const val script = "import foo\nval x = foo { 0 }"

class ReplCompilerJava8Test : TestCase() {

    var tmpdir : File? = null

    private var disposable: Disposable? = null

    override fun setUp() {
        super.setUp()
        tmpdir = KotlinTestUtils.tmpDirForTest(this)

        File(tmpdir, "library.kt").writeText(library)

        disposable = Disposer.newDisposable()

        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.FULL_JDK).apply {
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false))
            addKotlinSourceRoot(tmpdir!!.absolutePath)
            put(JVMConfigurationKeys.OUTPUT_DIRECTORY, tmpdir!!)
            put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)
        }

        val environment = KotlinCoreEnvironment.createForProduction(disposable!!, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        val res = KotlinToJVMBytecodeCompiler.compileBunchOfSources(environment)
        Assert.assertTrue(res)
    }

    override fun tearDown() {
        Disposer.dispose(disposable!!)
    }

    @Test
    fun testIncompatibleScriptJvmTargetConfig() {

        val configuration = makeConfiguration().apply {
            put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_6)
        }
        try {
            runTest(configuration)
            Assert.fail("Should fail due to bytecode incompatibility check")
        }
        catch (e: CompilationException) {
            Assert.assertTrue(e.message!!.contains("This compiler can only inline Java 1.6 bytecode (version 50)"))
        }
    }

    @Test
    fun testIncompatibleScriptJvmTargetProperty() {

        val configuration = makeConfiguration()
        System.setProperty(KOTLIN_REPL_JVM_TARGET_PROPERTY, "1.6")
        try {
            runTest(configuration)
            Assert.fail("Should fail due to bytecode incompatibility check")
        }
        catch (e: CompilationException) {
            Assert.assertTrue(e.message!!.contains("This compiler can only inline Java 1.6 bytecode (version 50)"))
        }
        finally {
            System.clearProperty(KOTLIN_REPL_JVM_TARGET_PROPERTY)
        }
    }

    @Test
    fun testCompatibleScriptJvmTargetJavaVersionDetect() {

        val configuration = makeConfiguration()
        val result = runTest(configuration)
        Assert.assertTrue(result is ReplCompileResult.CompiledClasses)
    }

    @Test
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

    private fun makeConfiguration() = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.FULL_JDK, File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-stdlib.jar"), tmpdir)

    private fun runTest(configuration: CompilerConfiguration): ReplCompileResult {
        val replCompiler = GenericReplCompiler(disposable!!, StandardScriptDefinition, configuration, PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false))
        val state = replCompiler.createState()

        return replCompiler.compile(state, ReplCodeLine(0, 0, script))
    }
}
