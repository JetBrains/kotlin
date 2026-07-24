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

package org.jetbrains.kotlin.jvm.compiler

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.checkers.setupLanguageVersionSettingsForCompilerTests
import org.jetbrains.kotlin.checkers.setupLanguageVersionSettingsForMultifileCompilerTests
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.javac.registerJavac
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.renderer.AnnotationArgumentsRenderingPolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.KotlinTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations
import org.jetbrains.kotlin.test.KotlinTestUtils.newConfiguration
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparatorAdaptor.validateAndCompareDescriptorWithFile
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.io.IOException
import java.lang.annotation.Retention

abstract class AbstractCompileJavaAgainstKotlinTest : TestCaseWithTmpdir() {
    protected fun doTestWithJavac(ktFilePath: String) {
        doTest(ktFilePath, true)
    }

    protected fun doTestWithoutJavac(ktFilePath: String) {
        doTest(ktFilePath, false)
    }

    protected open fun doTest(ktFilePath: String, useJavac: Boolean) {
        assertTrue(ktFilePath.endsWith(".kt"))
        val ktFilePath = ForTestCompileRuntime.transformTestDataPath(ktFilePath).absolutePath
        val ktFile = File(ktFilePath)
        val javaFile = File(ktFilePath.replaceFirst("\\.kt$".toRegex(), ".java"))

        val javaErrorFile = File(ktFilePath.replaceFirst("\\.kt$".toRegex(), ".javaerr.txt"))

        val out = File(tmpdir, "out")

        val directives = KotlinTestUtils.parseDirectives(ktFile.readText())
        if (directives.contains("IGNORE_FIR")) return

        val compiledSuccessfully = if (useJavac) {
            compileKotlinWithJava(
                listOf(javaFile),
                listOf(ktFile),
                out, testRootDisposable
            )
        } else {
            val result = KotlinTestUtils.compileKotlinWithJava(
                listOf(javaFile),
                listOf(ktFile),
                out, testRootDisposable, this::updateConfiguration
            )
            if (!javaErrorFile.exists()) {
                result.assertSuccessful()
            } else {
                val errors = if (result is JavaCompilationResult.Failure) result.diagnostics else ""
                JUnit5Assertions.assertEqualsToFile(javaErrorFile, errors)
            }
            result == JavaCompilationResult.Success
        }

        if (!compiledSuccessfully) return

        val configuration = newConfiguration(
            ConfigurationKind.ALL, TestJdkKind.FULL_JDK,
            KtTestUtil.getAnnotationsJar(), out)
        configuration.put(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING, true)

        @OptIn(CoreEnvironmentDeprecation::class)
        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        setupLanguageVersionSettingsForCompilerTests(ktFile.readText(), environment)

        @Suppress("DEPRECATION_ERROR")
        val analysisResult = JvmResolveUtil.analyze(environment)
        val packageView = analysisResult.moduleDescriptor.getPackage(LoadDescriptorUtil.TEST_PACKAGE_FQNAME)
        assertFalse(packageView.isEmpty()) { "Nothing found in package ${LoadDescriptorUtil.TEST_PACKAGE_FQNAME}" }

        val expectedFile = File(ktFilePath.replaceFirst("\\.kt$".toRegex(), ".txt"))
        validateAndCompareDescriptorWithFile(packageView, CONFIGURATION, expectedFile)
    }

    fun updateConfiguration(configuration: CompilerConfiguration) {
        configuration.useLightTree = true
        configuration.useFir = true
    }

    @Throws(IOException::class)
    fun compileKotlinWithJava(
        javaFiles: List<File>,
        ktFiles: List<File>,
        outDir: File,
        disposable: Disposable
    ): Boolean {
        val environment = createEnvironmentWithMockJdkAndIdeaAnnotations(disposable)
        setupLanguageVersionSettingsForMultifileCompilerTests(ktFiles, environment)
        environment.configuration.put(JVMConfigurationKeys.USE_JAVAC, true)
        environment.configuration.put(JVMConfigurationKeys.COMPILE_JAVA, true)
        environment.configuration.put(JVMConfigurationKeys.OUTPUT_DIRECTORY, outDir)
        @OptIn(MessageCollectorAccess::class) // write access
        environment.configuration.messageCollector = MessageCollector.NONE
        updateConfiguration(environment.configuration)
        environment.registerJavac(
            javaFiles = javaFiles,
            kotlinFiles = listOf(KotlinTestUtils.loadKtFile(environment.project, ktFiles.first()))
        )
        if (!ktFiles.isEmpty()) {
            LoadDescriptorUtil.compileKotlinToDirAndGetModule(ktFiles, outDir, environment)
        } else {
            val mkdirs = outDir.mkdirs()
            assert(mkdirs) { "Not created: $outDir" }
        }
        return JavacWrapper.getInstance(environment.project).use { it.compile() }
    }

    companion object {
        // Do not render parameter names because there are test cases where classes inherit from JDK collections,
        // and some versions of JDK have debug information in the class files (including parameter names), and some don't
        private val CONFIGURATION = AbstractLoadJavaTest.COMPARATOR_CONFIGURATION.withRenderer(
            DescriptorRenderer.withOptions {
                withDefinedIn = false
                parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
                verbose = true
                annotationArgumentsRenderingPolicy = AnnotationArgumentsRenderingPolicy.UNLESS_EMPTY
                excludedAnnotationClasses = setOf(FqName(Retention::class.java.name))
                modifiers = DescriptorRendererModifier.ALL
            }
        )
    }
}
