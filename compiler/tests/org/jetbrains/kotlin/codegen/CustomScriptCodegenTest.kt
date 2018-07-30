/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.script.util.scriptCompilationClasspathFromContextOrStlib
import org.jetbrains.kotlin.scripting.compiler.plugin.configureScriptDefinitions
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_SCRIPTING_COMMON_JAR
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_SCRIPTING_COMPILER_PLUGIN_JAR
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_SCRIPTING_JVM_JAR
import java.io.File
import kotlin.reflect.KClass
import kotlin.script.experimental.annotations.KotlinScript


class CustomScriptCodegenTest : CodegenTestCase() {

    fun testAnnotatedDefinition() {
        createScriptTestEnvironment("org.jetbrains.kotlin.codegen.TestScriptWithAnnotatedBaseClass")
        loadScript("val x = 1")
        val res = generateScriptClass()
        assertNull(res.safeGetAnnotation(KotlinScript::class))
        assertNotNull(res.safeGetAnnotation(MyScriptClassAnnotation::class))
        assertNotNull(res.getConstructor().safeGetAnnotation(MyScriptConstructorAnnotation::class))
    }

    private fun generateScriptClass(): Class<*> = generateClass("ScriptTest")

    private fun loadScript(text: String) {
        myFiles = CodegenTestFiles.create("scriptTest.kts", text, myEnvironment.project)
    }

    private fun createScriptTestEnvironment(vararg scriptDefinitions: String) {
        if (myEnvironment != null) {
            throw IllegalStateException("must not set up myEnvironment twice")
        }

        additionalDependencies =
                scriptCompilationClasspathFromContextOrStlib("tests-common", "kotlin-stdlib") +
                File(TestScriptWithReceivers::class.java.protectionDomain.codeSource.location.toURI().path) +
                with(PathUtil.kotlinPathsForDistDirectory) {
                    arrayOf(
                        KOTLIN_SCRIPTING_COMPILER_PLUGIN_JAR, KOTLIN_SCRIPTING_COMMON_JAR,
                        KOTLIN_SCRIPTING_JVM_JAR
                    ).mapNotNull { File(libPath, it).also { assertTrue("$it not found", it.exists()) } }
                }

        val configuration = createConfiguration(
            ConfigurationKind.ALL,
            TestJdkKind.MOCK_JDK,
            additionalDependencies,
            emptyList(),
            emptyList()
        )
        if (scriptDefinitions.isNotEmpty()) {
            configureScriptDefinitions(
                scriptDefinitions.asList(), configuration, this::class.java.classLoader, MessageCollector.NONE, emptyMap()
            )
        }

        myEnvironment = KotlinCoreEnvironment.createForTests(
            testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }

}

private fun Class<*>.safeGetAnnotation(ann: KClass<out Annotation>): Annotation? =
    getAnnotation(classLoader.loadClass(ann.qualifiedName) as Class<Annotation>)

private fun java.lang.reflect.Constructor<*>.safeGetAnnotation(ann: KClass<out Annotation>): Annotation? =
    getAnnotation(this.declaringClass.classLoader.loadClass(ann.qualifiedName) as Class<Annotation>)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MyScriptClassAnnotation

@Target(AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.RUNTIME)
annotation class MyScriptConstructorAnnotation

@Suppress("unused")
@KotlinScript
@MyScriptClassAnnotation()
abstract class TestScriptWithAnnotatedBaseClass @MyScriptConstructorAnnotation constructor()

