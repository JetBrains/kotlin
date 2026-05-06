/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.setupKlibAbiCompatibilityLevel
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.extensionsStorage
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.compiler.plugin.TEST_ONLY_PLUGIN_REGISTRATION_CALLBACK
import org.jetbrains.kotlin.compiler.plugin.TEST_ONLY_PROJECT_CONFIGURATION_CALLBACK
import org.jetbrains.kotlin.compiler.plugin.registerInProject
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.config.enableIrNestedOffsetsChecks
import org.jetbrains.kotlin.config.enableIrVarargTypesChecks
import org.jetbrains.kotlin.config.enableIrVisibilityChecks
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.targetPlatform
import org.jetbrains.kotlin.config.useFir
import org.jetbrains.kotlin.config.verifyIr
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.isApplicableTo
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.utils.MessageCollectorForCompilerTests

open class CompilerConfigurationProviderImpl(
    testServices: TestServices,
    override val testRootDisposable: Disposable,
    override val configurators: List<AbstractEnvironmentConfigurator>
) : CompilerConfigurationProvider(testServices) {
    private val environmentCache: MutableMap<TestModule, KotlinCoreEnvironment> = mutableMapOf()
    private val configurationCache: MutableMap<Pair<TestModule, CompilationStage>, CompilerConfiguration> = mutableMapOf()

    override fun getKotlinCoreEnvironment(module: TestModule): KotlinCoreEnvironment {
        return environmentCache.getOrPut(module) {
            createKotlinCoreEnvironment(module)
        }
    }

    protected open fun createKotlinCoreEnvironment(module: TestModule): KotlinCoreEnvironment {
        val platform = module.targetPlatform(testServices)
        val configFiles = platform.platformToEnvironmentConfigFiles()
        val applicationEnvironment = KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForTests(
            testRootDisposable,
            CompilerConfiguration.create()
        )
        val configuration = getCompilerConfiguration(module, CompilationStage.FIRST)
        val projectEnv = KotlinCoreEnvironment.ProjectEnvironment(testRootDisposable, applicationEnvironment, configuration)
        return KotlinCoreEnvironment.createForTests(
            projectEnv,
            configuration,
            configFiles
        ).also {
            val extensionStorage = it.configuration.extensionsStorage!!
            registerCompilerExtensions(extensionStorage, module, configuration)
            if (!testServices.cliBasedFacadesEnabled) {
                configureProject(projectEnv.project, module, configuration)
                extensionStorage.registerInProject(projectEnv.project)
            }
        }
    }

    @OptIn(TestInfrastructureInternals::class)
    override fun getCompilerConfiguration(module: TestModule, compilationStage: CompilationStage): CompilerConfiguration {
        return configurationCache.getOrPut(module to compilationStage) { createCompilerConfiguration(module, compilationStage) }
    }

    @TestInfrastructureInternals
    fun createCompilerConfiguration(module: TestModule, compilationStage: CompilationStage): CompilerConfiguration {
        return createCompilerConfiguration(testServices, module, configurators, compilationStage).also { configuration ->
            if (testServices.cliBasedFacadesEnabled) {
                configuration.put(TEST_ONLY_PLUGIN_REGISTRATION_CALLBACK) { extensionStorage ->
                    registerCompilerExtensions(extensionStorage, module, configuration)
                }
                configuration.put(TEST_ONLY_PROJECT_CONFIGURATION_CALLBACK) {
                    configureProject(it, module, configuration)
                }
            }
        }
    }

    private fun TargetPlatform.platformToEnvironmentConfigFiles() = when {
        isJvm() -> EnvironmentConfigFiles.JVM_CONFIG_FILES
        isJs() -> EnvironmentConfigFiles.JS_CONFIG_FILES
        isNative() -> EnvironmentConfigFiles.NATIVE_CONFIG_FILES
        isWasm() -> EnvironmentConfigFiles.WASM_CONFIG_FILES
        // TODO: is it correct?
        isCommon() -> EnvironmentConfigFiles.METADATA_CONFIG_FILES
        else -> error("Unknown platform: $this")
    }
}

@TestInfrastructureInternals
fun createCompilerConfiguration(
    testServices: TestServices,
    module: TestModule,
    configurators: List<AbstractEnvironmentConfigurator>,
    compilationStage: CompilationStage,
): CompilerConfiguration {
    val configuration = CompilerConfiguration.create()
    configuration[CommonConfigurationKeys.MODULE_NAME] = module.name

    if (testServices.defaultsProvider.frontendKind == FrontendKinds.FIR) {
        configuration.useFir = true
    }

    configuration.verifyIr = IrVerificationMode.ERROR
    configuration.enableIrVisibilityChecks = !CodegenTestDirectives.DISABLE_IR_VISIBILITY_CHECKS.isApplicableTo(module, testServices)
    configuration.enableIrVarargTypesChecks = !CodegenTestDirectives.DISABLE_IR_VARARG_TYPE_CHECKS.isApplicableTo(module, testServices)

    configuration.enableIrNestedOffsetsChecks = CodegenTestDirectives.ENABLE_IR_NESTED_OFFSETS_CHECKS in module.directives &&
            !CodegenTestDirectives.DISABLE_IR_NESTED_OFFSETS_CHECKS.isApplicableTo(module, testServices)

    val messageCollector = MessageCollectorForCompilerTests(System.err, CompilerTestMessageRenderer(module))
    configuration.messageCollector = messageCollector
    configuration.languageVersionSettings = module.languageVersionSettings
    configuration.targetPlatform = module.targetPlatform(testServices)
    configuration.setupKlibAbiCompatibilityLevel()

    for (configurator in configurators) {
        if (compilationStage == configurator.compilationStage) {
            configurator.configureCompileConfigurationWithAdditionalConfigurationKeys(configuration, module)
        }
    }

    return configuration
}

private operator fun <T : Any> CompilerConfiguration.set(key: CompilerConfigurationKey<T>, value: T) {
    put(key, value)
}
