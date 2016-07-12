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

package org.jetbrains.kotlin.scripts

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.addKotlinSourceRoot
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.ScriptParameter
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import org.junit.Test

class ScriptTest {
    @Test
    @Throws(Exception::class)
    fun testScript() {
        val aClass = compileScript("fib.kts", TestScriptDefinition(".kts", numIntParam()))
        Assert.assertNotNull(aClass)
        aClass!!.getConstructor(Integer.TYPE).newInstance(4)
    }

    @Test
    @Throws(Exception::class)
    fun testScriptWithPackage() {
        val aClass = compileScript("fib.pkg.kts", TestScriptDefinition(".kts", numIntParam()))
        Assert.assertNotNull(aClass)
        aClass!!.getConstructor(Integer.TYPE).newInstance(4)
    }

    @Test
    @Throws(Exception::class)
    fun testScriptWithScriptDefinition() {
        val aClass = compileScript("fib.fib.kt", TestScriptDefinition(".fib.kt", numIntParam()))
        Assert.assertNotNull(aClass)
        aClass!!.getConstructor(Integer.TYPE).newInstance(4)
    }

    private fun compileScript(
            scriptPath: String,
            scriptDefinition: KotlinScriptDefinition): Class<*>? {
        val paths = PathUtil.getKotlinPathsForDistDirectory()
        val messageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)

        val rootDisposable = Disposer.newDisposable()
        try {
            val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.JDK_ONLY, TestJdkKind.FULL_JDK)
            configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            configuration.addKotlinSourceRoot("compiler/testData/script/" + scriptPath)
            configuration.add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, scriptDefinition)

            val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

            try {
                return KotlinToJVMBytecodeCompiler.compileScript(environment, paths)
            }
            catch (e: CompilationException) {
                messageCollector.report(CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(e),
                                        MessageUtil.psiElementToMessageLocation(e.element))
                return null
            }
            catch (t: Throwable) {
                MessageCollectorUtil.reportException(messageCollector, t)
                return null
            }

        }
        finally {
            Disposer.dispose(rootDisposable)
        }
    }

    private fun numIntParam(): List<ScriptParameter> {
        return listOf(ScriptParameter(Name.identifier("num"), DefaultBuiltIns.Instance.intType))
    }
}
