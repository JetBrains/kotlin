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
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.scripts.TestKotlinScriptDependenciesResolver
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.templates.ScriptTemplateDefinition

class ScriptGenTest : CodegenTestCase() {
    companion object {
        private val FIB_SCRIPT_DEFINITION =
            ScriptDefinition.FromLegacy(
                defaultJvmScriptingHostConfiguration,
                KotlinScriptDefinitionFromAnnotatedTemplate(ScriptWithIntParam::class)
            )
        private val NO_PARAM_SCRIPT_DEFINITION =
            ScriptDefinition.FromLegacy(
                defaultJvmScriptingHostConfiguration,
                KotlinScriptDefinitionFromAnnotatedTemplate(Any::class)
            )
    }

    override fun setUp() {
        super.setUp()
        additionalDependencies =
            System.getenv("PROJECT_CLASSES_DIRS")?.split(File.pathSeparator)?.map { File(it) }
                ?: listOf(
                    "compiler/build/classes/kotlin/test",
                    "build/compiler/classes/kotlin/test",
                    "out/test/compiler.test",
                    "out/test/compiler_test"
                )
                    .mapNotNull { File(it).canonicalFile.takeIf(File::isDirectory) }
                    .takeIf { it.isNotEmpty() }
                        ?: throw IllegalStateException("Unable to get classes output dirs, set PROJECT_CLASSES_DIRS environment variable")
    }

    fun testLanguage() {
        setUpEnvironment("scriptCustom/fib.lang.kts")

        val aClass = generateClass("Fib_lang")
        val constructor = aClass.getConstructor(Integer.TYPE)
        val result = aClass.getDeclaredField("result")
        result.isAccessible = true
        val script = constructor.newInstance(5)
        assertEquals(8, result.get(script))
    }

    fun testLanguageWithPackage() {
        setUpEnvironment("scriptCustom/fibwp.lang.kts")

        val aClass = generateClass("test.Fibwp_lang")
        val constructor = aClass.getConstructor(Integer.TYPE)
        val result = aClass.getDeclaredField("result")
        result.isAccessible = true
        val script = constructor.newInstance(5)
        assertEquals(8, result.get(script))
    }

    fun testDependentScripts() {
        setUpEnvironment(listOf("scriptCustom/fibwp.lang.kts", "scriptCustom/fibwprunner.kts"))

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
        setUpEnvironment("scriptCustom/methodWithClosure.lang.kts")

        val aClass = generateClass("MethodWithClosure_lang")
        val constructor = aClass.getConstructor(Integer.TYPE)
        val script = constructor.newInstance(239)
        val fib = aClass.getMethod("method")
        val invoke = fib.invoke(script)
        assertEquals(239, invoke as Int / 2)
    }

    fun testNameSanitation() {
        setUpEnvironment("scriptCustom/1#@2.kts")

        val aClass = generateClass("_1__2")
        assertEquals("OK", aClass.getDeclaredMethod("getResult")(aClass.newInstance()))
    }

    private fun setUpEnvironment(sourcePath: String) {
        setUpEnvironment(listOf(sourcePath))
    }

    private fun setUpEnvironment(sourcePaths: List<String>) {
        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.FULL_JDK).apply {
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false))
            add(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS, FIB_SCRIPT_DEFINITION)
            add(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS, NO_PARAM_SCRIPT_DEFINITION)
            put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)

            addKotlinSourceRoots(sourcePaths.map { "${KotlinTestUtils.getTestDataPathBase()}/codegen/$it" })
            addJvmClasspathRoots(additionalDependencies)
        }
        loadScriptingPlugin(configuration)

        myEnvironment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        loadFiles(*sourcePaths.toTypedArray())
    }
}

@Suppress("unused")
@ScriptTemplateDefinition(
    scriptFilePattern = ".*\\.lang\\.kts",
    resolver = TestKotlinScriptDependenciesResolver::class
)
abstract class ScriptWithIntParam(val num: Int)
