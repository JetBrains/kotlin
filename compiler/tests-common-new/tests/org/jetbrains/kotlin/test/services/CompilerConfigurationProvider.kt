/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.ApplicationEnvironmentDisposer
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import java.io.File

abstract class CompilerConfigurationProvider : TestService {
    abstract val testRootDisposable: Disposable

    protected abstract fun getKotlinCoreEnvironment(module: TestModule): KotlinCoreEnvironment

    fun getProject(module: TestModule): Project {
        return getKotlinCoreEnvironment(module).project
    }

    fun getPackagePartProviderFactory(module: TestModule): (GlobalSearchScope) -> JvmPackagePartProvider {
        return getKotlinCoreEnvironment(module)::createPackagePartProvider
    }

    fun getCompilerConfiguration(module: TestModule): CompilerConfiguration {
        return getKotlinCoreEnvironment(module).configuration
    }

    fun registerJavacForModule(module: TestModule, ktFiles: List<KtFile>, mockJdk: File?) {
        val environment = getKotlinCoreEnvironment(module)
        val bootClasspath = mockJdk?.let { listOf(it) }
        environment.registerJavac(kotlinFiles = ktFiles, bootClasspath = bootClasspath)
    }
}

val TestServices.compilerConfigurationProvider: CompilerConfigurationProvider by TestServices.testServiceAccessor()

open class CompilerConfigurationProviderImpl(
    override val testRootDisposable: Disposable,
    val configurators: List<EnvironmentConfigurator>
) : CompilerConfigurationProvider() {
    private val cache: MutableMap<TestModule, KotlinCoreEnvironment> = mutableMapOf()

    override fun getKotlinCoreEnvironment(module: TestModule): KotlinCoreEnvironment {
        return cache.getOrPut(module) {
            createKotlinCoreEnvironment(module)
        }
    }

    @OptIn(TestInfrastructureInternals::class)
    protected open fun createKotlinCoreEnvironment(module: TestModule): KotlinCoreEnvironment {
        val platform = module.targetPlatform
        val configFiles = when {
            platform.isJvm() -> EnvironmentConfigFiles.JVM_CONFIG_FILES
            platform.isJs() -> EnvironmentConfigFiles.JS_CONFIG_FILES
            platform.isNative() -> EnvironmentConfigFiles.NATIVE_CONFIG_FILES
            // TODO: is it correct?
            platform.isCommon() -> EnvironmentConfigFiles.METADATA_CONFIG_FILES
            else -> error("Unknown platform: $platform")
        }
        val applicationEnvironment = KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForTests(
            ApplicationEnvironmentDisposer.ROOT_DISPOSABLE,
            CompilerConfiguration()
        )
        val initialConfiguration = createCompilerConfiguration(module)
        val projectEnv = KotlinCoreEnvironment.ProjectEnvironment(testRootDisposable, applicationEnvironment, initialConfiguration)
        val project = projectEnv.project
        return KotlinCoreEnvironment.createForTests(
            projectEnv,
            initialConfiguration,
            configFiles
        ).also { configurators.forEach { it.registerCompilerExtensions(project) } }
    }

    @TestInfrastructureInternals
    fun createCompilerConfiguration(module: TestModule): CompilerConfiguration {
        val configuration = CompilerConfiguration()
        configuration[CommonConfigurationKeys.MODULE_NAME] = module.name

        if (module.frontendKind == FrontendKinds.FIR) {
            configuration[CommonConfigurationKeys.USE_FIR] = true
        }

        configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY] = object : MessageCollector {
            override fun clear() {}

            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
                if (severity == CompilerMessageSeverity.ERROR) {
                    val prefix = if (location == null) "" else "(" + location.path + ":" + location.line + ":" + location.column + ") "
                    throw AssertionError(prefix + message)
                }
            }

            override fun hasErrors(): Boolean = false
        }
        configuration.languageVersionSettings = module.languageVersionSettings

        configurators.forEach { it.configureCompileConfigurationWithAdditionalConfigurationKeys(configuration, module) }

        return configuration
    }

    private operator fun <T : Any> CompilerConfiguration.set(key: CompilerConfigurationKey<T>, value: T) {
        put(key, value)
    }
}

val TestModule.javaFiles: List<TestFile>
    get() = files.filter { it.isJavaFile }
