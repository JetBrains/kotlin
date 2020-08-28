/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.compiler.plugin.configureScriptDefinitions
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_SCRIPTING_COMMON_JAR
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_SCRIPTING_COMPILER_IMPL_JAR
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_SCRIPTING_COMPILER_PLUGIN_JAR
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_SCRIPTING_JVM_JAR
import org.junit.Assert
import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.api.providedProperties
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContextOrStdlib

abstract class AbstractCustomScriptCodegenTest : CodegenTestCase() {
    private lateinit var scriptDefinitions: List<String>

    override fun setUp() {
        super.setUp()

        configurationKind = ConfigurationKind.ALL
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        super.updateConfiguration(configuration)
        if (scriptDefinitions.isNotEmpty()) {
            configureScriptDefinitions(
                scriptDefinitions, configuration, this::class.java.classLoader, MessageCollector.NONE, defaultJvmScriptingHostConfiguration
            )
        }

        configuration.addJvmClasspathRoots(additionalDependencies.orEmpty())

        loadScriptingPlugin(configuration)
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        if (files.size > 1) {
            throw UnsupportedOperationException("Multiple files are not yet supported in this test")
        }

        val file = files.single()
        val content = file.content

        scriptDefinitions = InTextDirectivesUtils.findListWithPrefixes(content, "KOTLIN_SCRIPT_DEFINITION:")
        if (scriptDefinitions.isNotEmpty()) {
            additionalDependencies =
                scriptCompilationClasspathFromContextOrStdlib("tests-common", "kotlin-stdlib") +
                        File(TestScriptWithReceivers::class.java.protectionDomain.codeSource.location.toURI().path) +
                        with(PathUtil.kotlinPathsForDistDirectory) {
                            arrayOf(
                                KOTLIN_SCRIPTING_COMPILER_PLUGIN_JAR, KOTLIN_SCRIPTING_COMPILER_IMPL_JAR,
                                KOTLIN_SCRIPTING_COMMON_JAR, KOTLIN_SCRIPTING_JVM_JAR
                            ).mapNotNull { File(libPath, it).takeIf(File::exists) }
                        }
        }

        createEnvironmentWithMockJdkAndIdeaAnnotations(configurationKind, files, TestJdkKind.FULL_JDK)

        myFiles = CodegenTestFiles.create(file.name, content, myEnvironment.project)

        try {
            val scriptClass = generateClass(myFiles.psiFile.script!!.fqName.asString())

            // TODO: add types to receivers, envVars and params
            val receivers = InTextDirectivesUtils.findListWithPrefixes(content, "receiver:")
            val environmentVars = extractAllKeyValPairs(content, "envVar:")
            val scriptParams = InTextDirectivesUtils.findListWithPrefixes(content, "param:")
            val scriptInstance = runScript(scriptClass, receivers, environmentVars, scriptParams)

            val expectedFields = extractAllKeyValPairs(content, "expected:")
            checkExpectedFields(expectedFields, scriptClass, scriptInstance)
        } catch (e: Throwable) {
            printReport(wholeFile)
            throw e
        }
    }

    private fun extractAllKeyValPairs(content: String, directive: String): Map<String, String> =
        InTextDirectivesUtils.findListWithPrefixes(content, directive).associate { line ->
            line.substringBefore('=') to line.substringAfter('=')
        }

    private fun runScript(scriptClass: Class<*>, receivers: List<Any?>, environmentVars: Map<String, Any?>, scriptParams: List<Any>): Any? {

        val ctorParams = arrayListOf<Any?>()
        ctorParams.addAll(scriptParams)
        ctorParams.addAll(receivers)
        ctorParams.addAll(environmentVars.values)

        val constructor = scriptClass.constructors[0]
        return constructor.newInstance(*ctorParams.toTypedArray())
    }

    private fun checkExpectedFields(expectedFields: Map<String, Any?>, scriptClass: Class<*>, scriptInstance: Any?) {
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
}

object TestScriptWithReceiversConfiguration : ScriptCompilationConfiguration(
    {
        implicitReceivers(String::class)
    })

@Suppress("unused")
@KotlinScript(compilationConfiguration = TestScriptWithReceiversConfiguration::class)
abstract class TestScriptWithReceivers

object TestScriptWithSimpleEnvVarsConfiguration : ScriptCompilationConfiguration(
    {
        providedProperties("stringVar1" to String::class)
    })

@Suppress("unused")
@KotlinScript(compilationConfiguration = TestScriptWithSimpleEnvVarsConfiguration::class)
abstract class TestScriptWithSimpleEnvVars

@Suppress("unused")
@KotlinScript(fileExtension = "customext")
abstract class TestScriptWithNonKtsExtension(val name: String)

@Suppress("unused")
@KotlinScript(filePathPattern = "(.*/)?pathPattern[0-9]\\..+")
abstract class TestScriptWithPathPattern(val name2: String)
