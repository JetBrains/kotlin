/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.checkers.ENABLE_JVM_PREVIEW
import org.jetbrains.kotlin.checkers.parseLanguageVersionSettings
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

abstract class KotlinBaseTest<F : KotlinBaseTest.TestFile> : KtUsefulTestCase() {
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
    }

    @Throws(java.lang.Exception::class)
    protected open fun doTest(filePath: String) {
        val file = File(filePath)
        val expectedText = KtTestUtil.doLoadFile(file)
        doMultiFileTest(file, createTestFilesFromFile(file, expectedText))
    }

    protected abstract fun createTestFilesFromFile(file: File, expectedText: String): List<F>

    @Throws(java.lang.Exception::class)
    protected open fun doMultiFileTest(
        wholeFile: File,
        files: List<F>
    ) {
        throw UnsupportedOperationException("Multi-file test cases are not supported in this test")
    }

    protected open fun getTestJdkKind(files: List<F>): TestJdkKind {
        if (files.any { file -> InTextDirectivesUtils.isDirectiveDefined(file.content, "JDK_17_0") }) return TestJdkKind.FULL_JDK_17

        for (file in files) {
            if (InTextDirectivesUtils.isDirectiveDefined(file.content, "FULL_JDK")) {
                return TestJdkKind.FULL_JDK
            }
        }
        return TestJdkKind.MOCK_JDK
    }

    protected open fun extractConfigurationKind(files: List<F>): ConfigurationKind {
        return Companion.extractConfigurationKind(files)
    }

    protected open fun updateConfiguration(configuration: CompilerConfiguration) {}

    protected open fun setupEnvironment(environment: KotlinCoreEnvironment) {}

    protected open fun parseDirectivesPerFiles() = false

    protected open val backend = TargetBackend.ANY

    protected open fun configureTestSpecific(configuration: CompilerConfiguration, testFiles: List<TestFile>) {}

    protected fun createConfiguration(
        kind: ConfigurationKind,
        jdkKind: TestJdkKind,
        backend: TargetBackend,
        classpath: List<File?>,
        javaSource: List<File?>,
        testFilesWithConfigurationDirectives: List<TestFile>
    ): CompilerConfiguration {
        val configuration = KotlinTestUtils.newConfiguration(kind, jdkKind, classpath, javaSource)
        configuration.put(JVMConfigurationKeys.IR, backend.isIR)
        updateConfigurationByDirectivesInTestFiles(
            testFilesWithConfigurationDirectives,
            configuration,
            parseDirectivesPerFiles()
        )
        updateConfiguration(configuration)
        configureTestSpecific(configuration, testFilesWithConfigurationDirectives)
        return configuration
    }

    open class TestFile @JvmOverloads constructor(
        @JvmField val name: String,
        @JvmField val content: String,
        @JvmField val directives: Directives = Directives()
    ) : Comparable<TestFile> {
        override operator fun compareTo(other: TestFile): Int {
            return name.compareTo(other.name)
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is TestFile && other.name == name
        }

        override fun toString(): String {
            return name
        }

    }

    open class TestModule(
        @JvmField val name: String,
        @JvmField val dependenciesSymbols: List<String>,
        @JvmField val friendsSymbols: List<String>,
        @JvmField val dependsOnSymbols: List<String> = listOf(), // mimics the name from ModuleStructureExtractorImpl, thought later converted to `-Xfragment-refines` parameter
    ) : Comparable<TestModule> {

        val dependencies: MutableList<TestModule> = arrayListOf()
        val friends: MutableList<TestModule> = arrayListOf()
        val dependsOn: MutableList<TestModule> = arrayListOf()

        override fun compareTo(other: TestModule): Int = name.compareTo(other.name)

        override fun toString(): String = name
    }

    companion object {
        @JvmStatic
        fun updateConfigurationByDirectivesInTestFiles(
            testFilesWithConfigurationDirectives: List<TestFile>,
            configuration: CompilerConfiguration
        ) {
            updateConfigurationByDirectivesInTestFiles(testFilesWithConfigurationDirectives, configuration, false)
        }


        private fun updateConfigurationByDirectivesInTestFiles(
            testFilesWithConfigurationDirectives: List<TestFile>,
            configuration: CompilerConfiguration,
            usePreparsedDirectives: Boolean
        ) {
            var explicitLanguageVersionSettings: LanguageVersionSettings? = null
            val kotlinConfigurationFlags: MutableList<String> = ArrayList(0)
            for (testFile in testFilesWithConfigurationDirectives) {
                val content = testFile.content
                val directives = if (usePreparsedDirectives) testFile.directives else KotlinTestUtils.parseDirectives(content)
                val flags = directives.listValues("KOTLIN_CONFIGURATION_FLAGS")
                if (flags != null) {
                    kotlinConfigurationFlags.addAll(flags)
                }
                val targetString = directives["JVM_TARGET"]
                if (targetString != null) {
                    val jvmTarget = JvmTarget.fromString(targetString)
                        ?: error("Unknown target: $targetString")
                    configuration.put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)
                }

                if (directives.contains(ENABLE_JVM_PREVIEW)) {
                    configuration.put(JVMConfigurationKeys.ENABLE_JVM_PREVIEW, true)
                }

                val version = directives["LANGUAGE_VERSION"]
                if (version != null) {
                    throw AssertionError(
                        """
                    Do not use LANGUAGE_VERSION directive in compiler tests because it's prone to limiting the test
                    to a specific language version, which will become obsolete at some point and the test won't check
                    things like feature intersection with newer releases. Use `// !LANGUAGE: [+-]FeatureName` directive instead,
                    where FeatureName is an entry of the enum `LanguageFeature`
                    
                    """.trimIndent()
                    )
                }
                val fileLanguageVersionSettings: LanguageVersionSettings? = parseLanguageVersionSettings(directives)
                if (fileLanguageVersionSettings != null) {
                    assert(explicitLanguageVersionSettings == null) { "Should not specify !LANGUAGE directive twice" }
                    explicitLanguageVersionSettings = fileLanguageVersionSettings
                }

                val lambdasString = directives["LAMBDAS"]
                if (lambdasString != null) {
                    val lambdas = JvmClosureGenerationScheme.fromString(lambdasString)
                        ?: error("Unknown lambdas mode: $lambdasString")
                    configuration.put(JVMConfigurationKeys.LAMBDAS, lambdas)
                }
            }
            if (explicitLanguageVersionSettings != null) {
                configuration.languageVersionSettings = explicitLanguageVersionSettings
            }
            updateConfigurationWithFlags(configuration, kotlinConfigurationFlags)
        }

        private fun updateConfigurationWithFlags(configuration: CompilerConfiguration, flags: List<String>) {
            val configurationFlags = parseAnalysisFlags(flags)
            configurationFlags.entries.forEach { (key, value) ->
                @Suppress("UNCHECKED_CAST")
                configuration.put(key as CompilerConfigurationKey<Any>, value)
            }
        }

        fun extractConfigurationKind(files: List<TestFile>): ConfigurationKind {
            var addRuntime = false
            var addReflect = false
            for (file in files) {
                if (InTextDirectivesUtils.isDirectiveDefined(file.content, "WITH_STDLIB")) {
                    addRuntime = true
                }
                if (InTextDirectivesUtils.isDirectiveDefined(file.content, "WITH_REFLECT")) {
                    addReflect = true
                }
            }
            return if (addReflect) ConfigurationKind.ALL else if (addRuntime) ConfigurationKind.NO_KOTLIN_REFLECT else ConfigurationKind.JDK_ONLY
        }

        fun getTestJdkKind(files: List<TestFile>): TestJdkKind {
            for (file in files) {
                if (InTextDirectivesUtils.isDirectiveDefined(file.content, "FULL_JDK")) {
                    return TestJdkKind.FULL_JDK
                }
            }
            return TestJdkKind.MOCK_JDK
        }
    }
}
