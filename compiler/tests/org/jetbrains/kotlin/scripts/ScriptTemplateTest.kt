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
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.addKotlinSourceRoot
import org.jetbrains.kotlin.daemon.TestMessageCollector
import org.jetbrains.kotlin.daemon.assertHasMessage
import org.jetbrains.kotlin.daemon.toFile
import org.jetbrains.kotlin.script.InvalidScriptResolverAnnotation
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.script.tryConstructClassFromStringArgs
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.util.KotlinFrontEndException
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.lang.Exception
import java.lang.reflect.InvocationTargetException
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.Future
import kotlin.reflect.KClass
import kotlin.script.dependencies.*
import kotlin.script.dependencies.DependenciesResolver.ResolveResult
import kotlin.script.dependencies.experimental.AsyncDependenciesResolver
import kotlin.script.templates.AcceptedAnnotations
import kotlin.script.templates.ScriptTemplateDefinition
import kotlin.script.templates.standard.ScriptTemplateWithArgs

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
        val savedErr = System.err
        try {
            System.setErr(PrintStream(NullOutputStream()))
            Assert.assertNull(compileScript("fib_ext_ann2.kts", ScriptWithIntParamAndDummyResolver::class, null, includeKotlinRuntime = false))
        }
        finally {
            System.setErr(savedErr)
        }

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
        val aClass = compileScript("fib_std.kts", ScriptTemplateWithArgs::class, runIsolated = false)
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
            val anObj =  tryConstructClassFromStringArgs(aClass!!, listOf("4"))
            Assert.assertNotNull(anObj)
        }.let {
            assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, it)
        }
    }

    @Test
    fun testScriptErrorReporting() {
        val messageCollector = TestMessageCollector()
        compileScript("fib.kts", ScriptReportingErrors::class, messageCollector = messageCollector)

        messageCollector.assertHasMessage("error", desiredSeverity = CompilerMessageSeverity.ERROR)
        messageCollector.assertHasMessage("warning", desiredSeverity = CompilerMessageSeverity.WARNING)
        messageCollector.assertHasMessage("info", desiredSeverity = CompilerMessageSeverity.INFO)
        messageCollector.assertHasMessage("debug", desiredSeverity = CompilerMessageSeverity.LOGGING)
    }

    @Test
    fun testAsyncResolver() {
        val aClass = compileScript("fib.kts", ScriptWithAsyncResolver::class, null)
        Assert.assertNotNull(aClass)
        val out = captureOut {
            aClass!!.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    @Test
    fun testAcceptedAnnotationsSync() {
        val aClass = compileScript("acceptedAnnotations.kts", ScriptWithAcceptedAnnotationsSyncResolver::class, null)
        Assert.assertNotNull(aClass)
    }

    @Test
    fun testAcceptedAnnotationsAsync() {
        val aClass = compileScript("acceptedAnnotations.kts", ScriptWithAcceptedAnnotationsAsyncResolver::class, null)
        Assert.assertNotNull(aClass)
    }

    @Test
    fun testAcceptedAnnotationsLegacy() {
        val aClass = compileScript("acceptedAnnotations.kts", ScriptWithAcceptedAnnotationsLegacyResolver::class, null)
        Assert.assertNotNull(aClass)
    }

    @Test
    fun testSmokeScriptException() {
        val aClass = compileScript("smoke_exception.kts", ScriptWithArrayParam::class)
        Assert.assertNotNull(aClass)
        var exceptionThrown = false
        try {
            tryConstructClassFromStringArgs(aClass!!, emptyList())
        }
        catch (e: InvocationTargetException) {
            Assert.assertTrue(e.cause is IllegalStateException)
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testScriptWithNoMatchingTemplate() {
        try {
            compileScript("fib.kts", ScriptWithDifferentFileNamePattern::class, null)
            Assert.fail("should throw compilation error")
        }
        catch (e: KotlinFrontEndException) {
            if (e.message?.contains("Should not parse a script without definition") != true) {
                // unexpected error
                throw e
            }
        }
    }

    private fun compileScript(
            scriptPath: String,
            scriptTemplate: KClass<out Any>,
            environment: Map<String, Any?>? = null,
            runIsolated: Boolean = true,
            messageCollector: MessageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false),
            includeKotlinRuntime: Boolean = true
    ): Class<*>? =
            compileScriptImpl("compiler/testData/script/" + scriptPath, KotlinScriptDefinitionFromAnnotatedTemplate(
                    scriptTemplate, null, null, environment
            ), runIsolated, messageCollector, includeKotlinRuntime)

    private fun compileScriptImpl(
            scriptPath: String,
            scriptDefinition: KotlinScriptDefinition,
            runIsolated: Boolean,
            messageCollector: MessageCollector,
            includeKotlinRuntime: Boolean
    ): Class<*>? {
        val rootDisposable = Disposer.newDisposable()
        try {
            val configuration = KotlinTestUtils.newConfiguration(if (includeKotlinRuntime) ConfigurationKind.ALL else ConfigurationKind.JDK_ONLY, TestJdkKind.FULL_JDK)
            configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            configuration.addKotlinSourceRoot(scriptPath)
            configuration.add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, scriptDefinition)
            configuration.put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)

            val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

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

open class TestKotlinScriptDummyDependenciesResolver : DependenciesResolver {

    @AcceptedAnnotations(DependsOn::class, DependsOnTwo::class)
    override fun resolve(scriptContents: ScriptContents,
                         environment: Environment
    ): ResolveResult
    {
        return ScriptDependencies(
            classpath = classpathFromClassloader(),
            imports = listOf("org.jetbrains.kotlin.scripts.DependsOn", "org.jetbrains.kotlin.scripts.DependsOnTwo")
        ).asSuccess()
    }
}

private fun classpathFromClassloader(): List<File> =
        (TestKotlinScriptDependenciesResolver::class.java.classLoader as? URLClassLoader)?.urLs
                ?.mapNotNull(URL::toFile)
                ?.filter { it.path.contains("out") && it.path.contains("test") }
        ?: emptyList()


open class TestKotlinScriptDependenciesResolver : TestKotlinScriptDummyDependenciesResolver() {

    private val kotlinPaths by lazy { PathUtil.getKotlinPathsForCompiler() }

    @AcceptedAnnotations(DependsOn::class, DependsOnTwo::class)
    override fun resolve(scriptContents: ScriptContents,
                         environment: Environment
    ): ResolveResult
    {
        val cp = scriptContents.annotations.flatMap {
            when (it) {
                is DependsOn -> if (it.path == "@{runtime}") listOf(kotlinPaths.runtimePath, kotlinPaths.scriptRuntimePath) else listOf(File(it.path))
                is DependsOnTwo -> listOf(it.path1, it.path2).flatMap {
                    when {
                        it.isBlank() -> emptyList()
                        it == "@{runtime}" -> listOf(kotlinPaths.runtimePath, kotlinPaths.scriptRuntimePath)
                        else -> listOf(File(it))
                    }
                }
                is InvalidScriptResolverAnnotation -> throw Exception("Invalid annotation ${it.name}", it.error)
                else -> throw Exception("Unknown annotation ${it::class.java}")
            }
        }
        return ScriptDependencies(
            classpath = classpathFromClassloader() + cp,
            imports = listOf("org.jetbrains.kotlin.scripts.DependsOn", "org.jetbrains.kotlin.scripts.DependsOnTwo")
        ).asSuccess()
    }
}

class TestParamClass(@Suppress("unused") val memberNum: Int)

class ErrorReportingResolver : TestKotlinScriptDependenciesResolver() {
    override fun resolve(
            scriptContents: ScriptContents,
            environment: Environment
    ): ResolveResult {
        return ResolveResult.Success(
                super.resolve(scriptContents, environment).dependencies!!,
                listOf(
                        ScriptReport("error", ScriptReport.Severity.ERROR, null),
                        ScriptReport("warning", ScriptReport.Severity.WARNING, ScriptReport.Position(1, 0)),
                        ScriptReport("info", ScriptReport.Severity.INFO, ScriptReport.Position(2, 0)),
                        ScriptReport("debug", ScriptReport.Severity.DEBUG, ScriptReport.Position(3, 0))

                )
        )
    }
}

class TestAsyncResolver : TestKotlinScriptDependenciesResolver(), AsyncDependenciesResolver {
    override suspend fun resolveAsync(
            scriptContents: ScriptContents,
            environment: Environment
    ): ResolveResult = super<TestKotlinScriptDependenciesResolver>.resolve(scriptContents, environment)

    override fun resolve(scriptContents: ScriptContents, environment: Environment): ResolveResult =
            super<AsyncDependenciesResolver>.resolve(scriptContents, environment)
}

@Target(AnnotationTarget.FILE)
annotation class TestAnno1
@Target(AnnotationTarget.FILE)
annotation class TestAnno2
@Target(AnnotationTarget.FILE)
annotation class TestAnno3

private val annotationFqNames = listOf(TestAnno1::class, TestAnno2::class, TestAnno3::class).map { it.qualifiedName!! }

interface AcceptedAnnotationsCheck {
    fun checkHasAnno1Annotation(scriptContents: ScriptContents): ResolveResult.Success {
        val actualAnnotations = scriptContents.annotations
        Assert.assertTrue(
                "Loaded annotation: $actualAnnotations",
                actualAnnotations.single().annotationClass.qualifiedName == TestAnno1::class.qualifiedName
        )

        return ScriptDependencies(
                classpath = classpathFromClassloader(),
                imports = annotationFqNames
        ).asSuccess()
    }
}

class TestAcceptedAnnotationsSyncResolver: DependenciesResolver, AcceptedAnnotationsCheck {
    @AcceptedAnnotations(TestAnno1::class, TestAnno3::class)
    override fun resolve(scriptContents: ScriptContents, environment: Environment): ResolveResult {
        return checkHasAnno1Annotation(scriptContents)
    }
}

class TestAcceptedAnnotationsAsyncResolver: AsyncDependenciesResolver, AcceptedAnnotationsCheck {
    @AcceptedAnnotations(TestAnno1::class, TestAnno3::class)
    override suspend fun resolveAsync(scriptContents: ScriptContents, environment: Environment): ResolveResult {
        return checkHasAnno1Annotation(scriptContents)
    }
}

class TestAcceptedAnnotationsLegacyResolver: ScriptDependenciesResolver, AcceptedAnnotationsCheck {
    @AcceptedAnnotations(TestAnno1::class, TestAnno3::class)
    override fun resolve(
            script: ScriptContents,
            environment: Environment?,
            report: (ScriptDependenciesResolver.ReportSeverity, String, ScriptContents.Position?) -> Unit,
            previousDependencies: KotlinScriptExternalDependencies?
    ): Future<KotlinScriptExternalDependencies?> {
        checkHasAnno1Annotation(script)
        return object : KotlinScriptExternalDependencies {
            override val classpath: Iterable<File>
                get() = classpathFromClassloader()

            override val imports: Iterable<String>
                get() = annotationFqNames
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

@ScriptTemplateDefinition(
        scriptFilePattern = ".*\\.custom\\.kts",
        resolver = TestKotlinScriptDependenciesResolver::class
)
abstract class ScriptWithDifferentFileNamePattern

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

@ScriptTemplateDefinition(resolver = ErrorReportingResolver::class)
abstract class ScriptReportingErrors(val num: Int)

@ScriptTemplateDefinition(resolver = TestAsyncResolver::class)
abstract class ScriptWithAsyncResolver(val num: Int)

@ScriptTemplateDefinition(resolver = TestAcceptedAnnotationsSyncResolver::class)
abstract class ScriptWithAcceptedAnnotationsSyncResolver

@ScriptTemplateDefinition(resolver = TestAcceptedAnnotationsAsyncResolver::class)
abstract class ScriptWithAcceptedAnnotationsAsyncResolver

@ScriptTemplateDefinition(resolver = TestAcceptedAnnotationsLegacyResolver::class)
abstract class ScriptWithAcceptedAnnotationsLegacyResolver

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class DependsOn(val path: String)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class DependsOnTwo(val unused: String = "", val path1: String = "", val path2: String = "")

private class NullOutputStream : OutputStream() {
    override fun write(b: Int) { }
    override fun write(b: ByteArray) { }
    override fun write(b: ByteArray, off: Int, len: Int) { }
}
