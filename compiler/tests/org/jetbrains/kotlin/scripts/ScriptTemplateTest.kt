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
import org.jetbrains.kotlin.cli.common.tryConstructScriptClass
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.addKotlinSourceRoot
import org.jetbrains.kotlin.daemon.toFile
import org.jetbrains.kotlin.script.*
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.lang.Exception
import java.lang.reflect.InvocationTargetException
import java.net.URISyntaxException
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.Future
import kotlin.reflect.KClass
import kotlin.script.StandardScriptTemplate

// TODO: the contetnts of this file should go into ScriptTest.kt and replace appropriate xml-based functionality,
// as soon as the the latter is removed from the codebase

class ScriptTemplateTest {
    @Test
    fun testScriptWithParam() {
        val aClass = compileScript("fib.kts", ScriptWithIntParam::class, null)
        Assert.assertNotNull(aClass)
        val out = captureOut {
            aClass!!.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    @Test
    fun testScriptWithClassParameter() {
        val aClass = compileScript("fib_cp.kts", ScriptWithClassParam::class, null, runIsolated = false)
        Assert.assertNotNull(aClass)
        val out = captureOut {
            aClass!!.getConstructor(TestParamClass::class.java).newInstance(TestParamClass(4))
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    @Test
    fun testScriptWithBaseClassWithParam() {
        val aClass = compileScript("fib_dsl.kts", ScriptWithBaseClass::class, null, runIsolated = false)
        Assert.assertNotNull(aClass)
        val out = captureOut {
            aClass!!.getConstructor(Integer.TYPE, Integer.TYPE).newInstance(4, 1)
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    @Test
    fun testScriptWithDependsAnn() {
        Assert.assertNull(compileScript("fib_ext_ann.kts", ScriptWithIntParamAndDummyResolver::class, null, includeKotlinRuntime = false))

        val aClass = compileScript("fib_ext_ann.kts", ScriptWithIntParam::class, null, includeKotlinRuntime = false)
        Assert.assertNotNull(aClass)
        val out = captureOut {
            aClass!!.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    @Test
    fun testScriptWithDependsAnn2() {
        Assert.assertNull(compileScript("fib_ext_ann2.kts", ScriptWithIntParamAndDummyResolver::class, null, includeKotlinRuntime = false))

        val aClass = compileScript("fib_ext_ann2.kts", ScriptWithIntParam::class, null, includeKotlinRuntime = false)
        Assert.assertNotNull(aClass)
        val out = captureOut {
            aClass!!.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    @Test
    fun testScriptWithoutParams() {
        val aClass = compileScript("without_params.kts", ScriptWithoutParams::class, null)
        Assert.assertNotNull(aClass)
        val out = captureOut {
            aClass!!.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed("10", out)
    }

    @Test
    fun testScriptWithOverriddenParam() {
        val aClass = compileScript("overridden_parameter.kts", ScriptBaseClassWithOverriddenProperty::class, null)
        Assert.assertNotNull(aClass)
        val out = captureOut {
            aClass!!.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed("14", out)
    }

    @Test
    fun testScriptWithArrayParam() {
        val aClass = compileScript("array_parameter.kts", ScriptWithArrayParam::class, null)
        Assert.assertNotNull(aClass)
        captureOut {
            aClass!!.getConstructor(Array<String>::class.java).newInstance(arrayOf("one", "two"))
        }.let {
            assertEqualsTrimmed("one and two", it)
        }
    }

    @Test
    fun testScriptWithNullableParam() {
        val aClass = compileScript("nullable_parameter.kts", ScriptWithNullableParam::class, null)
        Assert.assertNotNull(aClass)
        captureOut {
            aClass!!.getConstructor(Int::class.javaObjectType).newInstance(null)
        }.let {
            assertEqualsTrimmed("Param is null", it)
        }
    }

    @Test
    fun testScriptVarianceParams() {
        val aClass = compileScript("variance_parameters.kts", ScriptVarianceParams::class, null)
        Assert.assertNotNull(aClass)
        captureOut {
            aClass!!.getConstructor(Array<in Number>::class.java, Array<out Number>::class.java).newInstance(arrayOf("one"), arrayOf(1, 2))
        }.let {
            assertEqualsTrimmed("one and 1", it)
        }
    }

    @Test
    fun testScriptWithNullableProjection() {
        val aClass = compileScript("nullable_projection.kts", ScriptWithNullableProjection::class, null)
        Assert.assertNotNull(aClass)
        captureOut {
            aClass!!.getConstructor(Array<String>::class.java).newInstance(arrayOf<String?>(null))
        }.let {
            assertEqualsTrimmed("nullable", it)
        }
    }

    @Test
    fun testScriptWithArray2DParam() {
        val aClass = compileScript("array2d_param.kts", ScriptWithArray2DParam::class, null)
        Assert.assertNotNull(aClass)
        captureOut {
            aClass!!.getConstructor(Array<Array<in String>>::class.java).newInstance(arrayOf(arrayOf("one"), arrayOf("two")))
        }.let {
            assertEqualsTrimmed("first: one, size: 1", it)
        }
    }

    @Test
    fun testScriptWithStandardTemplate() {
        val aClass = compileScript("fib_std.kts", StandardScriptTemplate::class, runIsolated = false)
        Assert.assertNotNull(aClass)
        captureOut {
            aClass!!.getConstructor(Array<String>::class.java).newInstance(arrayOf("4", "other"))
        }.let {
            assertEqualsTrimmed(NUM_4_LINE + " (other)" + FIB_SCRIPT_OUTPUT_TAIL, it)
        }
    }

    @Test
    fun testScriptWithPackage() {
        val aClass = compileScript("fib.pkg.kts", ScriptWithIntParam::class)
        Assert.assertNotNull(aClass)
        captureOut {
            aClass!!.getConstructor(Integer.TYPE).newInstance(4)
        }.let {
            assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, it)
        }
    }

    @Test
    fun testScriptWithScriptDefinition() {
        val aClass = compileScript("fib.kts", ScriptWithIntParam::class)
        Assert.assertNotNull(aClass)
        captureOut {
            aClass!!.getConstructor(Integer.TYPE).newInstance(4)
        }.let {
            assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, it)
        }
    }

    @Test
    fun testScriptWithParamConversion() {
        val aClass = compileScript("fib.kts", ScriptWithIntParam::class)
        Assert.assertNotNull(aClass)
        captureOut {
            val anObj =  tryConstructScriptClass(aClass!!, listOf("4"))
            Assert.assertNotNull(anObj)
        }.let {
            assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, it)
        }
    }

    @Test
    fun testSmokeScriptException() {
        val aClass = compileScript("smoke_exception.kts", ScriptWithArrayParam::class)
        Assert.assertNotNull(aClass)
        var exceptionThrown = false
        try {
            tryConstructScriptClass(aClass!!, emptyList())
        }
        catch (e: InvocationTargetException) {
            Assert.assertTrue(e.cause is IllegalStateException)
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }

    private fun compileScript(
            scriptPath: String,
            scriptTemplate: KClass<out Any>,
            environment: Map<String, Any?>? = null,
            runIsolated: Boolean = true,
            suppressOutput: Boolean = false,
            includeKotlinRuntime: Boolean = true): Class<*>? =
            compileScriptImpl("compiler/testData/script/" + scriptPath, KotlinScriptDefinitionFromAnnotatedTemplate(scriptTemplate, null, null, environment), runIsolated, suppressOutput, includeKotlinRuntime)

    private fun compileScriptImpl(
            scriptPath: String,
            scriptDefinition: KotlinScriptDefinition,
            runIsolated: Boolean,
            suppressOutput: Boolean,
            includeKotlinRuntime: Boolean): Class<*>?
    {
        val paths = PathUtil.getKotlinPathsForDistDirectory()
        val messageCollector =
                if (suppressOutput) MessageCollector.NONE
                else PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)

        val rootDisposable = Disposer.newDisposable()
        try {
            val configuration = KotlinTestUtils.newConfiguration(if (includeKotlinRuntime) ConfigurationKind.ALL else ConfigurationKind.JDK_ONLY, TestJdkKind.FULL_JDK)
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

open class TestKotlinScriptDummyDependenciesResolver : ScriptDependenciesResolver {

    @AcceptedAnnotations(DependsOn::class, DependsOnTwo::class)
    override fun resolve(script: ScriptContents,
                         environment: Map<String, Any?>?,
                         report: (ScriptDependenciesResolver.ReportSeverity, String, ScriptContents.Position?) -> Unit,
                         previousDependencies: KotlinScriptExternalDependencies?
    ): Future<KotlinScriptExternalDependencies?>
    {
        return object : KotlinScriptExternalDependencies {
            override val classpath: Iterable<File> = classpathFromClassloader()
            override val imports: Iterable<String> = listOf("org.jetbrains.kotlin.scripts.DependsOn", "org.jetbrains.kotlin.scripts.DependsOnTwo")
        }.asFuture()
    }

    protected fun classpathFromClassloader(): List<File> =
            (TestKotlinScriptDependenciesResolver::class.java.classLoader as? URLClassLoader)?.urLs
                    ?.mapNotNull(URL::toFile)
                    ?.filter { it.path.contains("out") && it.path.contains("test") }
            ?: emptyList()
}

class TestKotlinScriptDependenciesResolver : TestKotlinScriptDummyDependenciesResolver() {

    private val kotlinPaths by lazy { PathUtil.getKotlinPathsForCompiler() }

    @AcceptedAnnotations(DependsOn::class, DependsOnTwo::class)
    override fun resolve(script: ScriptContents,
                         environment: Map<String, Any?>?,
                         report: (ScriptDependenciesResolver.ReportSeverity, String, ScriptContents.Position?) -> Unit,
                         previousDependencies: KotlinScriptExternalDependencies?
    ): Future<KotlinScriptExternalDependencies?>
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
        }.asFuture()
    }
}

@ScriptTemplateDefinition(
        scriptFilePattern =".*\\.kts",
        resolver = TestKotlinScriptDummyDependenciesResolver::class)
abstract class ScriptWithIntParamAndDummyResolver(val num: Int)

@ScriptTemplateDefinition(
        scriptFilePattern =".*\\.kts",
        resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithIntParam(val num: Int)

@ScriptTemplateDefinition(
        scriptFilePattern =".*\\.kts",
        resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithClassParam(val param: TestParamClass)

@ScriptTemplateDefinition(
        scriptFilePattern =".*\\.kts",
        resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithBaseClass(val num: Int, passthrough: Int) : TestDSLClassWithParam(passthrough)

@ScriptTemplateDefinition(
        scriptFilePattern =".*\\.kts",
        resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithoutParams(num: Int)

@ScriptTemplateDefinition(
        scriptFilePattern =".*\\.kts",
        resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptBaseClassWithOverriddenProperty(override val num: Int) : TestClassWithOverridableProperty(num)

@ScriptTemplateDefinition(resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithArrayParam(val myArgs: Array<String>)

@ScriptTemplateDefinition(resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithNullableParam(val param: Int?)

@ScriptTemplateDefinition(resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptVarianceParams(val param1: Array<in Number>, val param2: Array<out Number>)

@ScriptTemplateDefinition(resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithNullableProjection(val param: Array<String?>)

@ScriptTemplateDefinition(resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithArray2DParam(val param: Array<Array<in String>>)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class DependsOn(val path: String)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class DependsOnTwo(val unused: String = "", val path1: String = "", val path2: String = "")
