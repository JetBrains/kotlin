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
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.script.*
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import org.junit.Test
import java.net.URLClassLoader
import kotlin.reflect.KClass

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

    private fun compileScript(
            scriptPath: String,
            scriptBase: KClass<out Any>,
            context: Any? = null,
            runIsolated: Boolean = true,
            suppressOutput: Boolean = false): Class<*>? =
            compileScriptImpl("compiler/testData/script/" + scriptPath, KotlinScriptDefinitionFromTemplate(scriptBase, context), runIsolated, suppressOutput)

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

class GetTestKotlinScriptDependencies : GetScriptDependencies {

    private val kotlinPaths by lazy { PathUtil.getKotlinPathsForCompiler() }

    override fun invoke(annotations: Iterable<KtAnnotationEntry>, context: Any?): KotlinScriptExternalDependencies? {
        if (annotations.none()) return null
        val anns = annotations.map { parseAnnotation(it) }.filter { it.name == depends::class.simpleName }
        val cp = anns.flatMap {
            it.value.mapNotNull {
                when (it) {
                    is SimpleUntypedAst.Node.str -> if (it.value == "@{runtime}") kotlinPaths.runtimePath.canonicalPath else it.value
                    else -> null
                }
            }
        }
        return object : KotlinScriptExternalDependencies {
            override val classpath = cp
        }
    }

    private fun classpathFromClassloader(): List<String> =
            (GetTestKotlinScriptDependencies::class.java.classLoader as? URLClassLoader)?.urLs
                    ?.mapNotNull { it.toFile()?.canonicalPath }
                    ?.filter { it.contains("out/test") }
            ?: emptyList()

    override fun invoke(context: Any?): KotlinScriptExternalDependencies? {
        return object : KotlinScriptExternalDependencies {
            override val classpath = classpathFromClassloader()
        }
    }
}

@ScriptFilePattern(".*\\.kts")
@ScriptDependencyResolver(GetTestKotlinScriptDependencies::class)
abstract class ScriptWithIntParam(num: Int)

@ScriptFilePattern(".*\\.kts")
@ScriptDependencyResolver(GetTestKotlinScriptDependencies::class)
abstract class ScriptWithClassParam(param: TestParamClass)

@ScriptFilePattern(".*\\.kts")
@ScriptDependencyResolver(GetTestKotlinScriptDependencies::class)
abstract class ScriptWithBaseClass(num: Int, passthrough: Int) : TestDSLClassWithParam(passthrough)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class depends(val path: String)
