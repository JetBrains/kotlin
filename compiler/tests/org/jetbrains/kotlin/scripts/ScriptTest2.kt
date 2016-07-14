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

package org.jetbrains.kotlin.scripts

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.addKotlinSourceRoot
import org.jetbrains.kotlin.script.*
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.Future
import kotlin.reflect.KClass

// TODO: the contetnts of this file should go into ScriptTest.kt and replace appropriate xml-based functionality,
// as soon as the the latter is removed from the codebase

class ScriptTest2 {
    @Test
    fun testScriptWithParam() {
        val aClass = compileScript("fib.kts", ScriptWithIntParam::class, null)
        Assert.assertNotNull(aClass)
        aClass!!.getConstructor(Integer.TYPE).newInstance(4)
    }

    @Test
    fun testScriptWithClassParameter() {
        val aClass = compileScript("fib_cp.kts", ScriptWithClassParam::class, null, runIsolated = false)
        Assert.assertNotNull(aClass)
        aClass!!.getConstructor(TestParamClass::class.java).newInstance(TestParamClass(4))
    }

    @Test
    fun testScriptWithBaseClassWithParam() {
        val aClass = compileScript("fib_dsl.kts", ScriptWithBaseClass::class, null, runIsolated = false)
        Assert.assertNotNull(aClass)
        aClass!!.getConstructor(Integer.TYPE, Integer.TYPE).newInstance(4, 1)
    }

    @Test
    fun testScriptWithDependsAnn() {
        val aClass = compileScript("fib_ext_ann.kts", ScriptWithIntParam::class, null)
        Assert.assertNotNull(aClass)
        aClass!!.getConstructor(Integer.TYPE).newInstance(4)
    }

    @Test
    fun testScriptWithDependsAnn2() {
        val aClass = compileScript("fib_ext_ann2.kts", ScriptWithIntParam::class, null)
        Assert.assertNotNull(aClass)
        aClass!!.getConstructor(Integer.TYPE).newInstance(4)
    }

    private fun compileScript(
            scriptPath: String,
            scriptBase: KClass<out Any>,
            environment: Map<String, Any?>? = null,
            runIsolated: Boolean = true,
            suppressOutput: Boolean = false): Class<*>? =
            compileScriptImpl("compiler/testData/script/" + scriptPath, KotlinScriptDefinitionFromTemplate(scriptBase, null, environment), runIsolated, suppressOutput)

    private fun compileScriptImpl(
            scriptPath: String,
            scriptDefinition: KotlinScriptDefinition,
            runIsolated: Boolean,
            suppressOutput: Boolean): Class<*>?
    {
        val paths = PathUtil.getKotlinPathsForDistDirectory()
        val messageCollector =
                if (suppressOutput) MessageCollector.NONE
                else PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)

        val rootDisposable = Disposer.newDisposable()
        try {
            val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.JDK_ONLY, TestJdkKind.FULL_JDK)
            configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            configuration.addKotlinSourceRoot(scriptPath)
            configuration.add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, scriptDefinition)
            configuration.put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)

            val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

            try {
                return if (runIsolated) KotlinToJVMBytecodeCompiler.compileScript(environment, paths)
                else KotlinToJVMBytecodeCompiler.compileScript(environment, this.javaClass.classLoader)
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

class TestKotlinScriptDependenciesResolver : ScriptDependenciesResolverEx {

    private val kotlinPaths by lazy { PathUtil.getKotlinPathsForCompiler() }

    @AcceptedAnnotations(DependsOn::class, DependsOnTwo::class)
    override fun resolve(script: ScriptContents,
                         environment: Map<String, Any?>?,
                         previousDependencies: KotlinScriptExternalDependencies?
    ): KotlinScriptExternalDependencies?
    {
        val cp = script.annotations.flatMap {
            when (it) {
                is DependsOn -> listOf(if (it.path == "@{runtime}") kotlinPaths.runtimePath else File(it.path))
                is DependsOnTwo -> listOf(it.path1, it.path2).mapNotNull {
                    when {
                        it.isBlank() -> null
                        it == "@{runtime}" -> kotlinPaths.runtimePath
                        else -> File(it)
                    }
                }
                is InvalidScriptResolverAnnotation -> throw Exception("Invalid annotation ${it.name}", it.error)
                else -> throw Exception("Unknown annotation ${it.javaClass}")
            }
        }
        return object : KotlinScriptExternalDependencies {
            override val classpath: Iterable<File> = classpathFromClassloader() + cp
            override val imports: Iterable<String> = listOf("org.jetbrains.kotlin.scripts.DependsOn", "org.jetbrains.kotlin.scripts.DependsOnTwo")
        }
    }

    private fun classpathFromClassloader(): List<File> =
            (TestKotlinScriptDependenciesResolver::class.java.classLoader as? URLClassLoader)?.urLs
                    ?.mapNotNull { it.toFile() }
                    ?.filter { it.path.contains("out") && it.path.contains("test") }
            ?: emptyList()
}

@ScriptTemplateDefinition(
        scriptFilePattern =".*\\.kts",
        resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithIntParam(num: Int)

@ScriptTemplateDefinition(
        scriptFilePattern =".*\\.kts",
        resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithClassParam(param: TestParamClass)

@ScriptTemplateDefinition(
        scriptFilePattern =".*\\.kts",
        resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithBaseClass(num: Int, passthrough: Int) : TestDSLClassWithParam(passthrough)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class DependsOn(val path: String)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class DependsOnTwo(val unused: String = "", val path1: String = "", val path2: String = "")
