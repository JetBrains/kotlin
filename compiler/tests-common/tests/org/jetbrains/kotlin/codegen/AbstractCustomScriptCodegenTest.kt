/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.script.util.scriptCompilationClasspathFromContextOrStlib
import org.jetbrains.kotlin.scripting.compiler.plugin.configureScriptDefinitions
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.junit.Assert
import java.io.File
import kotlin.reflect.full.starProjectedType
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.annotations.KotlinScriptDefaultCompilationConfiguration
import kotlin.script.experimental.api.ScriptCompileConfigurationProperties
import kotlin.script.experimental.misc.invoke
import kotlin.script.experimental.util.TypedKey

/*
   Note by Ilya Chernikov: I gave up attempts to reuse the main compilation and testing logic from CodegenTestCase: it is too rigid
   and ad-hoc. I ended up reimplementing (with some copy/paste) a subset relevant for scripting, but in (I assume) much more composable
   and generic way.
   I suggest that we will start building a parallel implementation of the CodegenTestCase taking composability much more seriously,
   avoiding shared state and other rotten OOP misfeatures as much as possible. This implementation could be a starting point, unless
   we'll find something better, of course.
 */

open class AbstractCustomScriptCodegenTest : CodegenTestCase() {

    // TODO: add types to receivers, envVars and params
    class ScriptTestFile(val file: File) {

        val content by lazy { file.readText() }

        val definitions: List<String> by lazy { extractAllSimpleValues("KOTLIN_SCRIPT_DEFINITION") }

        val receivers: List<Any?> by lazy { extractAllSimpleValues("receiver") }

        val environmentVars: Map<String, Any?> by lazy { extractAllKeyValPairs("envVar") }

        val expected: Map<String, Any?> by lazy { extractAllKeyValPairs("expected") }

        val scriptParams: List<Any> by lazy { extractAllSimpleValues("param") }

        private fun extractAllSimpleValues(directive: String) =
            Regex("//\\s*$directive:\\s*([^\\s]+)").findAll(content).map { it.groupValues[1] }.toList()

        private fun extractAllKeyValPairs(directive: String) =
            Regex("//\\s*$directive:\\s*([^\\s]+)\\s*=\\s*([^\\s]+)").findAll(content).map { it.groupValues[1] to it.groupValues[2] }.toMap()
    }

    fun createEnvironment(
        kind: ConfigurationKind,
        jdkKind: TestJdkKind,
        scriptDefinitions: List<String>
    ): KotlinCoreEnvironment {

        val classpath =
            if (scriptDefinitions.isNotEmpty()) {
                scriptCompilationClasspathFromContextOrStlib("tests-common", "kotlin-stdlib").also {
                    additionalDependencies = it
                }
            } else {
                emptyList()
            }

        val configuration = KotlinTestUtils.newConfiguration(kind, jdkKind, classpath, emptyList())

        if (scriptDefinitions.isNotEmpty()) {
            configureScriptDefinitions(scriptDefinitions, configuration, MessageCollector.NONE, emptyMap())
        }

        return KotlinCoreEnvironment.createForTests(
            testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).also {
            myEnvironment = it
        }
    }

    override fun doTest(filename: String) {
        val testFile = AbstractCustomScriptCodegenTest.ScriptTestFile(File(filename))
        val environment = createEnvironment(ConfigurationKind.ALL, TestJdkKind.FULL_JDK, testFile.definitions)

        val psiFile = KotlinTestUtils.createFile(testFile.file.name, testFile.content, environment.project).also {
            val ranges = AnalyzingUtils.getSyntaxErrorRanges(it)
            assert(ranges.isEmpty()) { "Syntax errors found in $filename: $ranges" }
        }

        val classesFactory = generateClasses(listOf(psiFile))
        if (classesFactory == null) {
            fail("No class file was generated for: $name")
        }

        try {
            val scriptClass = classesFactory!!.loadClass(psiFile.script!!.fqName.asString())

            val scriptInstance = runScript(scriptClass, testFile.receivers, testFile.environmentVars, testFile.scriptParams)

            checkExpectedFields(testFile.expected, scriptClass, scriptInstance)

        } catch (e: Throwable) {
            println(classesFactory!!.createText())
            throw e
        }
    }

    private fun runScript(scriptClass: Class<*>, receivers: List<Any?>, environmentVars: Map<String, Any?>, scriptParams: List<Any>): Any? {

        val ctorParams = arrayListOf<Any>()
        if (receivers.isNotEmpty()) {
            ctorParams.add(receivers.toTypedArray())
        }
        if (environmentVars.isNotEmpty()) {
            ctorParams.add(environmentVars)
        }
        ctorParams.addAll(scriptParams)

        val constructor = scriptClass.constructors[0]
        return constructor.newInstance(*ctorParams.toTypedArray())
    }

    protected fun checkExpectedFields(expectedFields: Map<String, Any?>, scriptClass: Class<*>, scriptInstance: Any?) {
        Assert.assertFalse("expecting at least one expectation", expectedFields.isEmpty())

        for ((fieldName, expectedValue) in expectedFields) {

            if (expectedValue == "<nofield>") {
                try {
                    scriptClass.getDeclaredField(fieldName)
                    Assert.fail("must have no field $fieldName")
                } catch (e: NoSuchFieldException) {
                    continue
                }
            }

            val field = scriptClass.getDeclaredField(fieldName)
            field.isAccessible = true
            val resultString = field.get(scriptInstance)?.toString() ?: "null"
            Assert.assertEquals("comparing field $fieldName", expectedValue, resultString)
        }
    }

    protected fun ClassFileFactory.makeClassloader(): ClassLoader =
        GeneratedClassLoader(
            this,
            when {
                configurationKind.withReflection -> ForTestCompileRuntime.runtimeAndReflectJarClassLoader()
                configurationKind.withCoroutines -> ForTestCompileRuntime.runtimeAndCoroutinesJarClassLoader()
                else -> ForTestCompileRuntime.runtimeJarClassLoader()
            },
            *classPathURLs
        )

    protected fun ClassFileFactory.loadClass(name: String): Class<*> =
        makeClassloader().also {
            if (!verifyAllFilesWithAsm(this, it)) {
                Assert.fail("Verification failed: see exceptions above")
            }
        }.loadClass(name)

    protected fun generateClasses(psiFiles: List<KtFile>): ClassFileFactory? {
        try {
            val generationState = GenerationUtils.compileFiles(
                psiFiles, myEnvironment, classBuilderFactory,
                NoScopeRecordCliBindingTrace()
            )
            val classFileFactory = generationState.factory

            if (verifyWithDex() && DxChecker.RUN_DX_CHECKER) {
                DxChecker.check(classFileFactory)
            }
            return classFileFactory
        } catch (e: Throwable) {
            e.printStackTrace()
            System.err.println("Generating instructions as text...")
            try {
                if (classFileFactory == null) {
                    System.err.println("Cannot generate text: exception was thrown during generation")
                } else {
                    System.err.println(classFileFactory.createText())
                }
            } catch (e1: Throwable) {
                System.err.println("Exception thrown while trying to generate text, the actual exception follows:")
                e1.printStackTrace()
                System.err.println("-----------------------------------------------------------------------------")
            }

            Assert.fail("See exceptions above")
            return null
        }
    }
}

object TestScriptWithReceiversConfiguration : ArrayList<Pair<TypedKey<*>, Any?>>(
    listOf(
        ScriptCompileConfigurationProperties.scriptImplicitReceivers(String::class.starProjectedType)
    )
)

@Suppress("unused")
@KotlinScript
@KotlinScriptDefaultCompilationConfiguration(TestScriptWithReceiversConfiguration::class)
abstract class TestScriptWithReceivers

object TestScriptWithSimpleEnvVarsConfiguration : ArrayList<Pair<TypedKey<*>, Any?>>(
    listOf(
        ScriptCompileConfigurationProperties.contextVariables("stringVar1" to String::class.starProjectedType)
    )
)

@Suppress("unused")
@KotlinScript
@KotlinScriptDefaultCompilationConfiguration(TestScriptWithSimpleEnvVarsConfiguration::class)
abstract class TestScriptWithSimpleEnvVars
