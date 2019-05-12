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

package org.jetbrains.kotlin.scripts

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.StandardScriptDefinition
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.utils.tryConstructClassFromStringArgs
import org.junit.Assert
import java.io.File
import java.net.URLClassLoader
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class ScriptTest : KtUsefulTestCase() {
    fun testStandardScriptWithParams() {
        val aClass = compileScript("fib_std.kts", StandardScriptDefinition)
        Assert.assertNotNull(aClass)
        val out = captureOut {
            val anObj = tryConstructClassFromStringArgs(aClass!!, listOf("4", "comment"))
            Assert.assertNotNull(anObj)
        }
        assertEqualsTrimmed(NUM_4_LINE + " (comment)" + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    fun testStandardScriptWithoutParams() {
        val aClass = compileScript("fib_std.kts", StandardScriptDefinition)
        Assert.assertNotNull(aClass)
        val out = captureOut {
            val anObj = tryConstructClassFromStringArgs(aClass!!, emptyList())
            Assert.assertNotNull(anObj)
        }
        assertEqualsTrimmed(NUM_4_LINE + " (none)" + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    fun testStandardScriptWithSaving() {
        val tmpdir = File(KotlinTestUtils.tmpDirForTest(this), "withSaving")
        tmpdir.mkdirs()
        val aClass = compileScript("fib_std.kts", StandardScriptDefinition, saveClassesDir = tmpdir)
        Assert.assertNotNull(aClass)
        val out1 = captureOut {
            val anObj = tryConstructClassFromStringArgs(aClass!!, emptyList())
            Assert.assertNotNull(anObj)
        }
        assertEqualsTrimmed(NUM_4_LINE + " (none)" + FIB_SCRIPT_OUTPUT_TAIL, out1)
        val savedClassLoader = URLClassLoader(arrayOf(tmpdir.toURI().toURL()), aClass!!.classLoader)
        val aClassSaved = savedClassLoader.loadClass(aClass.name)
        Assert.assertNotNull(aClassSaved)
        val out2 = captureOut {
            val anObjSaved = tryConstructClassFromStringArgs(aClassSaved!!, emptyList())
            Assert.assertNotNull(anObjSaved)
        }
        assertEqualsTrimmed(NUM_4_LINE + " (none)" + FIB_SCRIPT_OUTPUT_TAIL, out2)
    }

    fun testUseCompilerInternals() {
        val scriptClass = compileScript("use_compiler_internals.kts", StandardScriptDefinition)!!
        assertEquals("OK", captureOut {
            tryConstructClassFromStringArgs(scriptClass, emptyList())!!
        })
    }

    private fun compileScript(
        scriptPath: String,
        scriptDefinition: KotlinScriptDefinition,
        runIsolated: Boolean = true,
        suppressOutput: Boolean = false,
        saveClassesDir: File? = null
    ): Class<*>? {
        val messageCollector =
                if (suppressOutput) MessageCollector.NONE
                else PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)

        val rootDisposable = Disposer.newDisposable()
        try {
            val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.FULL_JDK)
            configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            configuration.addKotlinSourceRoot("compiler/testData/script/$scriptPath")
            configuration.add(
                ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
                ScriptDefinition.FromLegacy(
                    defaultJvmScriptingHostConfiguration,
                    scriptDefinition
                )
            )
            configuration.put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)
            if (saveClassesDir != null) {
                configuration.put(JVMConfigurationKeys.OUTPUT_DIRECTORY, saveClassesDir)
            }

            loadScriptingPlugin(configuration)

            val environment = KotlinCoreEnvironment.createForTests(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

            try {
                return KotlinToJVMBytecodeCompiler.compileScript(environment, this::class.java.classLoader.takeUnless { runIsolated })
            }
            catch (e: CompilationException) {
                messageCollector.report(CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(e),
                                        MessageUtil.psiElementToMessageLocation(e.element))
                return null
            }
            catch (t: Throwable) {
                MessageCollectorUtil.reportException(messageCollector, t)
                throw t
            }
        }
        finally {
            Disposer.dispose(rootDisposable)
        }
    }
}
