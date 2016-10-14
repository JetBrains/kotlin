/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.addKotlinSourceRoots
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.script.ScriptTemplateDefinition
import org.jetbrains.kotlin.scripts.ScriptWithIntParam
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.org.objectweb.asm.Opcodes

class ScriptGenTest : CodegenTestCase() {
    companion object {
        private val FIB_SCRIPT_DEFINITION = KotlinScriptDefinitionFromAnnotatedTemplate(ScriptWithIntParam::class, providedScriptFilePattern = ".*\\.lang\\.kt")
        private val NO_PARAM_SCRIPT_DEFINITION = KotlinScriptDefinitionFromAnnotatedTemplate(Any::class, providedScriptFilePattern = ".*\\.kts")
    }

    override fun setUp() {
        super.setUp()
    }

    fun testLanguage() {
        setUpEnvironment("scriptCustom/fib.lang.kt")

        val aClass = generateClass("Fib_lang")
        val constructor = aClass.getConstructor(Integer.TYPE)
        val result = aClass.getDeclaredField("result")
        result.isAccessible = true
        val script = constructor.newInstance(5)
        assertEquals(8, result.get(script))
    }

    fun testLanguageWithPackage() {
        setUpEnvironment("scriptCustom/fibwp.lang.kt")

        val aClass = generateClass("test.Fibwp_lang")
        val constructor = aClass.getConstructor(Integer.TYPE)
        val result = aClass.getDeclaredField("result")
        result.isAccessible = true
        val script = constructor.newInstance(5)
        assertEquals(8, result.get(script))
    }

    fun testDependentScripts() {
        setUpEnvironment(listOf("scriptCustom/fibwp.lang.kt", "scriptCustom/fibwprunner.kts"))

        val aClass = generateClass("Fibwprunner")
        val constructor = aClass.getConstructor()
        val result = aClass.getDeclaredField("result")
        result.isAccessible = true
        val resultMethod = aClass.getDeclaredMethod("getResult")
        assertTrue(resultMethod.modifiers and Opcodes.ACC_FINAL != 0)
        assertTrue(resultMethod.modifiers and Opcodes.ACC_PUBLIC != 0)
        assertTrue(result.modifiers and Opcodes.ACC_PRIVATE != 0)
        val script = constructor.newInstance()
        assertEquals(8, result.get(script))
        assertEquals(8, resultMethod.invoke(script))
    }

    fun testScriptWhereMethodHasClosure() {
        setUpEnvironment("scriptCustom/methodWithClosure.lang.kt")

        val aClass = generateClass("MethodWithClosure_lang")
        val constructor = aClass.getConstructor(Integer.TYPE)
        val script = constructor.newInstance(239)
        val fib = aClass.getMethod("method")
        val invoke = fib.invoke(script)
        assertEquals(239, invoke as Int / 2)
    }

    private fun setUpEnvironment(sourcePath: String) {
        setUpEnvironment(listOf(sourcePath))
    }

    private fun setUpEnvironment(sourcePaths: List<String>) {
        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.JDK_ONLY, TestJdkKind.FULL_JDK).apply {
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false))
            add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, FIB_SCRIPT_DEFINITION)
            add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, NO_PARAM_SCRIPT_DEFINITION)
            put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)

            addKotlinSourceRoots(sourcePaths.map { "${KotlinTestUtils.getTestDataPathBase()}/codegen/$it" })
        }

        myEnvironment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        loadFiles(*sourcePaths.toTypedArray())
    }
}
