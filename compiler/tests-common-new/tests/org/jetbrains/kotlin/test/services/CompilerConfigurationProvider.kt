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
import org.jetbrains.kotlin.cli.common.messages.IrMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.registerInProject
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import java.io.File

abstract class CompilerConfigurationProvider(val testServices: TestServices) : TestService {
    abstract val testRootDisposable: Disposable
    abstract val configurators: List<AbstractEnvironmentConfigurator>

    protected abstract fun getKotlinCoreEnvironment(module: TestModule): KotlinCoreEnvironment

    open fun getProject(module: TestModule): Project {
        return getKotlinCoreEnvironment(module).project
    }

    fun registerCompilerExtensions(project: Project, module: TestModule, configuration: CompilerConfiguration) {
        val extensionStorage = CompilerPluginRegistrar.ExtensionStorage()
        for (configurator in configurators) {
            configurator.legacyRegisterCompilerExtensions(project, module, configuration)
            with(configurator) {
                extensionStorage.registerCompilerExtensions(module, configuration)
            }
        }
        extensionStorage.registerInProject(project)
    }

    open fun getPackagePartProviderFactory(module: TestModule): (GlobalSearchScope) -> JvmPackagePartProvider {
        return getKotlinCoreEnvironment(module)::createPackagePartProvider
    }

    open fun getCompilerConfiguration(module: TestModule): CompilerConfiguration {
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
    testServices: TestServices,
    override val testRootDisposable: Disposable,
    override val configurators: List<AbstractEnvironmentConfigurator>
) : CompilerConfigurationProvider(testServices) {
    private val cache: MutableMap<TestModule, KotlinCoreEnvironment> = mutableMapOf()

    override fun getKotlinCoreEnvironment(module: TestModule): KotlinCoreEnvironment {
        return cache.getOrPut(module) {
            createKotlinCoreEnvironment(module)
        }
    }

    @OptIn(TestInfrastructureInternals::class)
    protected open fun createKotlinCoreEnvironment(module: TestModule): KotlinCoreEnvironment {
        val platform = module.targetPlatform
        val configFiles = platform.platformToEnvironmentConfigFiles()
        val applicationEnvironment = KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForTests(
            testRootDisposable,
            CompilerConfiguration()
        )
        val configuration = createCompilerConfiguration(module, configurators)
        val projectEnv = KotlinCoreEnvironment.ProjectEnvironment(testRootDisposable, applicationEnvironment, configuration)
        return KotlinCoreEnvironment.createForTests(
            projectEnv,
            configuration,
            configFiles
        ).also { registerCompilerExtensions(projectEnv.project, module, configuration) }
    }


    @OptIn(TestInfrastructureInternals::class)
    fun createCompilerConfiguration(module: TestModule): CompilerConfiguration {
        return createCompilerConfiguration(module, configurators)
    }
}


@TestInfrastructureInternals
fun TargetPlatform.platformToEnvironmentConfigFiles() = when {
    isJvm() -> EnvironmentConfigFiles.JVM_CONFIG_FILES
    isJs() -> EnvironmentConfigFiles.JS_CONFIG_FILES
    isNative() -> EnvironmentConfigFiles.NATIVE_CONFIG_FILES
    isWasm() -> EnvironmentConfigFiles.WASM_CONFIG_FILES
    // TODO: is it correct?
    isCommon() -> EnvironmentConfigFiles.METADATA_CONFIG_FILES
    else -> error("Unknown platform: ${this}")
}

@TestInfrastructureInternals
fun createCompilerConfiguration(module: TestModule, configurators: List<AbstractEnvironmentConfigurator>): CompilerConfiguration {
    val configuration = CompilerConfiguration()
    configuration[CommonConfigurationKeys.MODULE_NAME] = module.name

    if (module.targetPlatform.isJvm() && CodegenTestDirectives.ENABLE_IR_FAKE_OVERRIDE_GENERATION in module.directives) {
        // For non-JVM platforms, the IR-based fake override builder is enabled unconditionally; on JVM it must be enabled manually.
        configuration.put(CommonConfigurationKeys.USE_IR_FAKE_OVERRIDE_BUILDER, true)
    }

    if (JsEnvironmentConfigurationDirectives.GENERATE_STRICT_IMPLICIT_EXPORT in module.directives) {
        configuration.put(JSConfigurationKeys.GENERATE_STRICT_IMPLICIT_EXPORT, true)
    }

    if (JsEnvironmentConfigurationDirectives.GENERATE_DTS in module.directives) {
        configuration.put(JSConfigurationKeys.GENERATE_DTS, true)
    }

    if (JsEnvironmentConfigurationDirectives.ES6_MODE in module.directives) {
        configuration.put(JSConfigurationKeys.USE_ES6_CLASSES, true)
        configuration.put(JSConfigurationKeys.COMPILE_SUSPEND_AS_JS_GENERATOR, true)
    }

    if (module.frontendKind == FrontendKinds.FIR) {
        configuration[CommonConfigurationKeys.USE_FIR] = true
    }

    val messageCollector = object : MessageCollector {
        override fun clear() {}

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            if (severity == CompilerMessageSeverity.ERROR) {
                val prefix = if (location == null) "" else "(" + location.path + ":" + location.line + ":" + location.column + ") "
                throw AssertionError(prefix + message)
            }
        }

        override fun hasErrors(): Boolean = false
    }
    configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY] = messageCollector
    configuration[IrMessageLogger.IR_MESSAGE_LOGGER] = IrMessageCollector(messageCollector)
    configuration.languageVersionSettings = module.languageVersionSettings

    configurators.forEach { it.configureCompileConfigurationWithAdditionalConfigurationKeys(configuration, module) }

    return configuration
}

private operator fun <T : Any> CompilerConfiguration.set(key: CompilerConfigurationKey<T>, value: T) {
    put(key, value)
}
