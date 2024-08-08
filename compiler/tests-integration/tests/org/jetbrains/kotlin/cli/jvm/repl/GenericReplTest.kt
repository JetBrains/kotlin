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

package org.jetbrains.kotlin.cli.jvm.repl

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.GenericReplCompiler
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.testFramework.resetApplicationToNull
import java.io.Closeable
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock

class GenericReplTest : KtUsefulTestCase() {
    fun testReplBasics() {
        TestRepl().use { repl ->
            val state = repl.createState()

            val res1 = repl.replCompiler.check(state, ReplCodeLine(0, 0, "val x ="))
            TestCase.assertTrue("Unexpected check results: $res1", res1 is ReplCheckResult.Incomplete)

            assertEvalResult(repl, state, "val l1 = listOf(1 + 2)\nl1.first()", 3)

            assertEvalUnit(repl, state, "val x = 5")

            assertEvalResult(repl, state, "x + 2", 7)
        }
    }

    fun testReplErrors() {
        TestRepl().use { repl ->
            val state = repl.createState()
            repl.compileAndEval(state, repl.nextCodeLine("val x = 10"))

            val res = repl.compileAndEval(state, repl.nextCodeLine("java.util.fish"))
            TestCase.assertTrue("Expected compile error", res.first is ReplCompileResult.Error)

            val result = repl.compileAndEval(state, repl.nextCodeLine("x"))
            assertEquals(res.second.toString(), 10, (result.second as? ReplEvalResult.ValueResult)?.value)
        }
    }

    fun testReplCodeFormat() {
        TestRepl().use { repl ->
            val state = repl.createState()

            val codeLine0 = ReplCodeLine(0, 0, "val l1 = 1\r\nl1\r\n")
            val res0 = repl.replCompiler.check(state, codeLine0)
            val res0c = res0 as? ReplCheckResult.Ok
            TestCase.assertNotNull("Unexpected compile result: $res0", res0c)
        }
    }

    fun testRepPackage() {
        TestRepl().use { repl ->
            val state = repl.createState()

            assertEvalResult(repl, state, "package mypackage\n\nval x = 1\nx+2", 3)

            assertEvalResult(repl, state, "x+4", 5)
        }
    }

    fun testCompilingReplEvaluator() {
        TestRepl().use { replBase ->
            val repl = GenericReplCompilingEvaluator(
                replBase.replCompiler, replBase.baseClasspath, Thread.currentThread().contextClassLoader,
                fallbackScriptArgs = replBase.emptyScriptArgs
            )

            val state = repl.createState()

            val res1 = repl.compileAndEval(state, ReplCodeLine(0, 0, "val x = 10"))
            assertTrue(res1 is ReplEvalResult.UnitResult)

            val res2 = repl.compileAndEval(state, ReplCodeLine(1, 0, "x"))
            assertEquals(res2.toString(), 10, (res2 as? ReplEvalResult.ValueResult)?.value)
        }
    }

    fun test256Evals() {
        TestRepl().use { repl ->
            val state = repl.createState()

            repl.compileAndEval(state, ReplCodeLine(0, 0, "val x0 = 0"))

            val evals = 256
            for (i in 1..evals) {
                repl.compileAndEval(state, ReplCodeLine(i, 0, "val x$i = x${i-1} + 1"))
            }

            val res = repl.compileAndEval(state, ReplCodeLine(evals + 1, 0, "x$evals"))
            assertEquals(res.second.toString(), evals, (res.second as? ReplEvalResult.ValueResult)?.value)
        }
    }

    fun testReplSlowdownKt22740() {
        TestRepl().use { repl ->
            val state = repl.createState()

            repl.compileAndEval(state, ReplCodeLine(0, 0, "class Test<T>(val x: T) { fun <R> map(f: (T) -> R): R = f(x) }".trimIndent()))

            // We expect that analysis time is not exponential
            for (i in 1..60) {
                repl.compileAndEval(state, ReplCodeLine(i, 0, "fun <T> Test<T>.map(f: (T) -> Double): List<Double> = listOf(f(this.x))"))
            }
        }
    }

    fun testReplResultFieldWithFunction() {
        TestRepl().use { repl ->
            val state = repl.createState()

            assertEvalResultIs<Function0<Int>>(repl, state, "{ 1 + 2 }")
            assertEvalResultIs<Function0<Int>>(repl, state, "res0")
            assertEvalResult(repl, state, "res0()", 3)
        }
    }

    fun testReplResultField() {
        TestRepl().use { repl ->
            val state = repl.createState()

            assertEvalResult(repl, state, "5 * 4", 20)
            assertEvalResult(repl, state, "res0 + 3", 23)
        }
    }

    private fun assertEvalUnit(repl: TestRepl, state: IReplStageState<*>, line: String) {
        val compiledClasses = checkCompile(repl, state, line)

        val evalResult = repl.compiledEvaluator.eval(state, compiledClasses!!)
        val unitResult = evalResult as? ReplEvalResult.UnitResult
        TestCase.assertNotNull("Unexpected eval result: $evalResult", unitResult)
    }

    private fun<R> assertEvalResult(repl: TestRepl, state: IReplStageState<*>, line: String, expectedResult: R) {
        val compiledClasses = checkCompile(repl, state, line)

        val evalResult = repl.compiledEvaluator.eval(state, compiledClasses!!)
        val valueResult = evalResult as? ReplEvalResult.ValueResult
        TestCase.assertNotNull("Unexpected eval result: $evalResult", valueResult)
        TestCase.assertEquals(expectedResult, valueResult!!.value)
    }

    private inline fun<reified R> assertEvalResultIs(repl: TestRepl, state: IReplStageState<*>, line: String) {
        val compiledClasses = checkCompile(repl, state, line)

        val evalResult = repl.compiledEvaluator.eval(state, compiledClasses!!)
        val valueResult = evalResult as? ReplEvalResult.ValueResult
        TestCase.assertNotNull("Unexpected eval result: $evalResult", valueResult)
        TestCase.assertTrue(valueResult!!.value is R)
    }

    private fun checkCompile(repl: TestRepl, state: IReplStageState<*>, line: String): ReplCompileResult.CompiledClasses? {
        val codeLine = repl.nextCodeLine(line)
        val compileResult = repl.replCompiler.compile(state, codeLine)
        val compiledClasses = compileResult as? ReplCompileResult.CompiledClasses
        TestCase.assertNotNull("Unexpected compile result: $compileResult", compiledClasses)
        return compiledClasses
    }
}


internal class TestRepl(
        templateClasspath: List<File> = listOf(File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-stdlib.jar")),
        templateClassName: String = "kotlin.script.templates.standard.ScriptTemplateWithArgs",
        repeatingMode: ReplRepeatingMode = ReplRepeatingMode.NONE
) : Closeable {
    val application = ApplicationManager.getApplication()

    private val disposable: Disposable by lazy { Disposer.newDisposable("${TestRepl::class.simpleName}.disposable") }

    val emptyScriptArgs = ScriptArgsWithTypes(arrayOf(emptyArray<String>()), arrayOf(Array<String>::class))

    private val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, *templateClasspath.toTypedArray()).apply {
        put(CommonConfigurationKeys.MODULE_NAME, "kotlin-script")
        loadScriptingPlugin(this)
    }

    val baseClasspath: List<File> get() = configuration.jvmClasspathRoots

    val currentLineCounter = AtomicInteger()

    fun nextCodeLine(code: String): ReplCodeLine = ReplCodeLine(currentLineCounter.getAndIncrement(), 0, code)

    private fun makeScriptDefinition(templateClasspath: List<File>, templateClassName: String): KotlinScriptDefinition {
        val classloader = URLClassLoader(templateClasspath.map { it.toURI().toURL() }.toTypedArray(), this::class.java.classLoader)
        val cls = classloader.loadClass(templateClassName)
        return KotlinScriptDefinitionFromAnnotatedTemplate(cls.kotlin, emptyMap())
    }

    private val scriptDef = makeScriptDefinition(templateClasspath, templateClassName)

    val replCompiler : GenericReplCompiler by lazy {
        GenericReplCompiler(disposable, scriptDef, configuration, PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false))
    }

    val compiledEvaluator: ReplEvaluator by lazy {
        GenericReplEvaluator(baseClasspath, Thread.currentThread().contextClassLoader, emptyScriptArgs, repeatingMode)
    }

    fun createState(lock: ReentrantReadWriteLock = ReentrantReadWriteLock()): IReplStageState<*> =
            AggregatedReplStageState(replCompiler.createState(lock), compiledEvaluator.createState(lock), lock)

    override fun close() {
        Disposer.dispose(disposable)
        resetApplicationToNull(application)
    }
}

private fun TestRepl.compileAndEval(state: IReplStageState<*>, codeLine: ReplCodeLine): Pair<ReplCompileResult, ReplEvalResult?> {

    val compRes = replCompiler.compile(state, codeLine)

    val evalRes = (compRes as? ReplCompileResult.CompiledClasses)?.let {

        compiledEvaluator.eval(state, it)
    }
    return compRes to evalRes
}
