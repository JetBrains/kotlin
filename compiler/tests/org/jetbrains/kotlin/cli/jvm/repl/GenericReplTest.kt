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
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase.resetApplicationToNull
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

            val codeLine0 = ReplCodeLine(0, 0, "val l1 = listOf(1 + 2)\nl1.first()")
            val res2 = repl.replCompiler.compile(state, codeLine0)
            val res2c = res2 as? ReplCompileResult.CompiledClasses
            TestCase.assertNotNull("Unexpected compile result: $res2", res2c)

            val res21 = repl.compiledEvaluator.eval(state, res2c!!)
            val res21e = res21 as? ReplEvalResult.ValueResult
            TestCase.assertNotNull("Unexpected eval result: $res21", res21e)
            TestCase.assertEquals(3, res21e!!.value)

            val codeLine1 = ReplCodeLine(1, 0, "val x = 5")
            val res3 = repl.replCompiler.compile(state, codeLine1)
            val res3c = res3 as? ReplCompileResult.CompiledClasses
            TestCase.assertNotNull("Unexpected compile result: $res3", res3c)

            val res31 = repl.compiledEvaluator.eval(state, res3c!!)
            val res31e = res31 as? ReplEvalResult.UnitResult
            TestCase.assertNotNull("Unexpected eval result: $res31", res31e)

            val codeLine2 = ReplCodeLine(2, 0, "x + 2")
            val res4 = repl.replCompiler.compile(state, codeLine2)
            val res4c = res4 as? ReplCompileResult.CompiledClasses
            TestCase.assertNotNull("Unexpected compile result: $res4", res4c)

            val res41 = repl.compiledEvaluator.eval(state, res4c!!)
            val res41e = res41 as? ReplEvalResult.ValueResult
            TestCase.assertNotNull("Unexpected eval result: $res41", res41e)
            TestCase.assertEquals(7, res41e!!.value)
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

            val codeLine1 = repl.nextCodeLine("package mypackage\n\nval x = 1\nx+2")
            val res1 = repl.replCompiler.compile(state, codeLine1)
            val res1c = res1 as? ReplCompileResult.CompiledClasses
            TestCase.assertNotNull("Unexpected compile result: $res1", res1c)

            val res11 = repl.compiledEvaluator.eval(state, res1c!!)
            val res11e = res11 as? ReplEvalResult.ValueResult
            TestCase.assertNotNull("Unexpected eval result: $res11", res11e)
            TestCase.assertEquals(3, res11e!!.value)

            val codeLine2 = repl.nextCodeLine("x+4")
            val res2 = repl.replCompiler.compile(state, codeLine2)
            val res2c = res2 as? ReplCompileResult.CompiledClasses
            TestCase.assertNotNull("Unexpected compile result: $res2", res2c)

            val res21 = repl.compiledEvaluator.eval(state, res2c!!)
            val res21e = res21 as? ReplEvalResult.ValueResult
            TestCase.assertNotNull("Unexpected eval result: $res21", res21e)
            TestCase.assertEquals(5, res21e!!.value)
        }
    }

    fun testCompilingReplEvaluator() {
        TestRepl().use { replBase ->
            val repl = GenericReplCompilingEvaluator(replBase.replCompiler, replBase.baseClasspath, fallbackScriptArgs = replBase.emptyScriptArgs)

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
}


internal class TestRepl(
        templateClasspath: List<File> = listOf(File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-stdlib.jar")),
        templateClassName: String = "kotlin.script.templates.standard.ScriptTemplateWithArgs",
        repeatingMode: ReplRepeatingMode = ReplRepeatingMode.NONE
) : Closeable {
    val application = ApplicationManager.getApplication()

    private val disposable: Disposable by lazy { Disposer.newDisposable() }

    val emptyScriptArgs = ScriptArgsWithTypes(arrayOf(emptyArray<String>()), arrayOf(Array<String>::class))

    private val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, *templateClasspath.toTypedArray()).apply {
        put(CommonConfigurationKeys.MODULE_NAME, "kotlin-script")
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
        GenericReplEvaluator(baseClasspath, null, emptyScriptArgs, repeatingMode)
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
