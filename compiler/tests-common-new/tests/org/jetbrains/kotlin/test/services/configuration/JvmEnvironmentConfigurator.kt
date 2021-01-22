/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import com.intellij.mock.MockProject
import com.intellij.openapi.util.SystemInfo
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.ASSERTIONS_MODE
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.CONSTRUCTOR_CALL_NORMALIZATION_MODE
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.STRING_CONCAT
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.DirectiveToConfigurationKeyExtractor
import org.jetbrains.kotlin.test.services.jvm.CompiledClassesManager
import org.jetbrains.kotlin.test.services.jvm.compiledClassesManager
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.test.util.joinToArrayString
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File

class JvmEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        val TEST_CONFIGURATION_KIND_KEY = CompilerConfigurationKey.create<ConfigurationKind>("ConfigurationKind")
    }

    override val directivesContainers: List<DirectivesContainer>
        get() = listOf(JvmEnvironmentConfigurationDirectives)

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::CompiledClassesManager))

    override fun DirectiveToConfigurationKeyExtractor.provideConfigurationKeys() {
        register(STRING_CONCAT, JVMConfigurationKeys.STRING_CONCAT)
        register(ASSERTIONS_MODE, JVMConfigurationKeys.ASSERTIONS_MODE)
        register(CONSTRUCTOR_CALL_NORMALIZATION_MODE, JVMConfigurationKeys.CONSTRUCTOR_CALL_NORMALIZATION_MODE)
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule, project: MockProject) {
        if (module.targetPlatform !in JvmPlatforms.allJvmPlatforms) return
        val registeredDirectives = module.directives
        val targets = registeredDirectives[JvmEnvironmentConfigurationDirectives.JVM_TARGET]
        when (targets.size) {
            0 -> {}
            1 -> configuration.put(JVMConfigurationKeys.JVM_TARGET, targets.single())
            else -> error("Too many jvm targets passed: ${targets.joinToArrayString()}")
        }

        when (extractJdkKind(registeredDirectives)) {
            TestJdkKind.MOCK_JDK -> {
                configuration.addJvmClasspathRoot(KtTestUtil.findMockJdkRtJar())
                configuration.put(JVMConfigurationKeys.NO_JDK, true)
            }
            TestJdkKind.MODIFIED_MOCK_JDK -> {
                configuration.addJvmClasspathRoot(KtTestUtil.findMockJdkRtModified())
                configuration.put(JVMConfigurationKeys.NO_JDK, true)
            }
            TestJdkKind.FULL_JDK_6 -> {
                val jdk6 = System.getenv("JDK_16") ?: error("Environment variable JDK_16 is not set")
                configuration.put(JVMConfigurationKeys.JDK_HOME, File(jdk6))
            }
            TestJdkKind.FULL_JDK_9 -> {
                configuration.put(JVMConfigurationKeys.JDK_HOME, KtTestUtil.getJdk9Home())
            }
            TestJdkKind.FULL_JDK_15 -> {
                configuration.put(JVMConfigurationKeys.JDK_HOME, KtTestUtil.getJdk15Home())
            }
            TestJdkKind.FULL_JDK -> {
                if (SystemInfo.IS_AT_LEAST_JAVA9) {
                    configuration.put(JVMConfigurationKeys.JDK_HOME, File(System.getProperty("java.home")))
                }
            }
            TestJdkKind.ANDROID_API -> {
                configuration.addJvmClasspathRoot(KtTestUtil.findAndroidApiJar())
                configuration.put(JVMConfigurationKeys.NO_JDK, true)
            }
        }

        val configurationKind = extractConfigurationKind(registeredDirectives).also {
            configuration.put(TEST_CONFIGURATION_KIND_KEY, it)
        }

        if (configurationKind.withRuntime) {
            configuration.addJvmClasspathRoot(ForTestCompileRuntime.runtimeJarForTests())
            configuration.addJvmClasspathRoot(ForTestCompileRuntime.scriptRuntimeJarForTests())
            configuration.addJvmClasspathRoot(ForTestCompileRuntime.kotlinTestJarForTests())
        } else if (configurationKind.withMockRuntime) {
            configuration.addJvmClasspathRoot(ForTestCompileRuntime.minimalRuntimeJarForTests())
            configuration.addJvmClasspathRoot(ForTestCompileRuntime.scriptRuntimeJarForTests())
        }
        if (configurationKind.withReflection) {
            configuration.addJvmClasspathRoot(ForTestCompileRuntime.reflectJarForTests())
        }
        configuration.addJvmClasspathRoot(KtTestUtil.getAnnotationsJar())

        if (JvmEnvironmentConfigurationDirectives.STDLIB_JDK8 in module.directives) {
            configuration.addJvmClasspathRoot(ForTestCompileRuntime.runtimeJarForTestsWithJdk8())
        }

        if (JvmEnvironmentConfigurationDirectives.ANDROID_ANNOTATIONS in module.directives) {
            configuration.addJvmClasspathRoot(ForTestCompileRuntime.androidAnnotationsForTests())
        }

        if (LanguageSettingsDirectives.ENABLE_JVM_PREVIEW in module.directives) {
            configuration.put(JVMConfigurationKeys.ENABLE_JVM_PREVIEW, true)
        }

        val isIr = module.targetBackend?.isIR == true
        configuration.put(JVMConfigurationKeys.IR, isIr)

        if (JvmEnvironmentConfigurationDirectives.SKIP_JAVA_SOURCES !in module.directives) {
            module.javaFiles.takeIf { it.isNotEmpty() }?.let { javaFiles ->
                javaFiles.forEach { testServices.sourceFileProvider.getRealFileForSourceFile(it) }
                configuration.addJavaSourceRoot(testServices.sourceFileProvider.javaSourceDirectory)
            }
        }

        configuration.registerModuleDependencies(module)

        if (JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING in module.directives) {
            configuration.put(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING, true)
        }

        initBinaryDependencies(module, configuration)
    }

    private fun extractJdkKind(registeredDirectives: RegisteredDirectives): TestJdkKind {
        val fullJdkEnabled = JvmEnvironmentConfigurationDirectives.FULL_JDK in registeredDirectives
        val jdkKinds = registeredDirectives[JvmEnvironmentConfigurationDirectives.JDK_KIND]

        if (fullJdkEnabled) {
            if (jdkKinds.isNotEmpty()) {
                error("FULL_JDK and JDK_KIND can not be used together")
            }
            return TestJdkKind.FULL_JDK
        }

        return when (jdkKinds.size) {
            0 -> TestJdkKind.MOCK_JDK
            1 -> jdkKinds.single()
            else -> error("Too many jdk kinds passed: ${jdkKinds.joinToArrayString()}")
        }
    }

    private fun extractConfigurationKind(registeredDirectives: RegisteredDirectives): ConfigurationKind {
        val withRuntime = JvmEnvironmentConfigurationDirectives.WITH_RUNTIME in registeredDirectives || JvmEnvironmentConfigurationDirectives.WITH_STDLIB in registeredDirectives
        val withReflect = JvmEnvironmentConfigurationDirectives.WITH_REFLECT in registeredDirectives
        val noRuntime = JvmEnvironmentConfigurationDirectives.NO_RUNTIME in registeredDirectives
        if (noRuntime && withRuntime) {
            error("NO_RUNTIME and WITH_RUNTIME can not be used together")
        }
        return when {
            withRuntime && !withReflect -> ConfigurationKind.NO_KOTLIN_REFLECT
            withRuntime || withReflect -> ConfigurationKind.ALL
            noRuntime -> ConfigurationKind.JDK_NO_RUNTIME
            else -> ConfigurationKind.JDK_ONLY
        }
    }

    private fun CompilerConfiguration.registerModuleDependencies(module: TestModule) {
        val dependencyProvider = testServices.dependencyProvider
        val modulesFromDependencies = module.dependencies
            .filter { it.kind == DependencyKind.Binary }
            .map { dependencyProvider.getTestModule(it.moduleName) }
            .takeIf { it.isNotEmpty() }
            ?: return
        val jarManager = testServices.compiledClassesManager
        val dependenciesClassPath = modulesFromDependencies.map { jarManager.getCompiledKotlinDirForModule(it) }
        addJvmClasspathRoots(dependenciesClassPath)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun initBinaryDependencies(
        module: TestModule,
        configuration: CompilerConfiguration,
    ) {
        val binaryDependencies = module.dependencies.filter { it.kind == DependencyKind.Binary }
        val binaryFriends = module.friends.filter { it.kind == DependencyKind.Binary }
        val dependencyProvider = testServices.dependencyProvider
        val compiledClassesManager = testServices.compiledClassesManager
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider

        fun addDependenciesToClasspath(dependencies: List<DependencyDescription>): List<File> {
            val jvmClasspathRoots = buildList<File> {
                dependencies.forEach {
                    val dependencyModule = dependencyProvider.getTestModule(it.moduleName)

                    add(compiledClassesManager.getCompiledKotlinDirForModule(dependencyModule))
                    addIfNotNull(compiledClassesManager.getCompiledJavaDirForModule(dependencyModule))
                    addAll(compilerConfigurationProvider.getCompilerConfiguration(dependencyModule).jvmClasspathRoots)
                }
            }
            configuration.addJvmClasspathRoots(jvmClasspathRoots)
            return jvmClasspathRoots
        }

        addDependenciesToClasspath(binaryDependencies)
        addDependenciesToClasspath(binaryFriends)

        if (binaryFriends.isNotEmpty()) {
            configuration.put(JVMConfigurationKeys.FRIEND_PATHS, binaryFriends.flatMap {
                val friendModule = dependencyProvider.getTestModule(it.moduleName)
                listOfNotNull(
                    compiledClassesManager.getCompiledKotlinDirForModule(friendModule),
                    compiledClassesManager.getCompiledJavaDirForModule(friendModule)
                )
            }.map { it.absolutePath })
        }
    }
}
