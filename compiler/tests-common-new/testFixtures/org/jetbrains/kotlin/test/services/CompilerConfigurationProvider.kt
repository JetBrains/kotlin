/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.TEST_ONLY_PLUGIN_REGISTRATION_CALLBACK
import org.jetbrains.kotlin.compiler.plugin.registerInProject
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.js.config.*
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
import org.jetbrains.kotlin.test.directives.isApplicableTo
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.utils.MessageCollectorForCompilerTests
import java.io.File

abstract class CompilerConfigurationProvider(val testServices: TestServices) : TestService {
    abstract val testRootDisposable: Disposable
    abstract val configurators: List<AbstractEnvironmentConfigurator>

    protected abstract fun getKotlinCoreEnvironment(module: TestModule): KotlinCoreEnvironment

    abstract fun getCompilerConfiguration(module: TestModule, compilationStage: CompilationStage): CompilerConfiguration

    context(handler: AnalysisHandler<*>)
    fun getCompilerConfiguration(module: TestModule): CompilerConfiguration =
        getCompilerConfiguration(module, handler.compilationStage)

    context(_: FrontendFacade<*>)
    fun getCompilerConfiguration(module: TestModule): CompilerConfiguration =
        getCompilerConfiguration(module, CompilationStage.FIRST)

    context(_: Frontend2BackendConverter<*, *>)
    fun getCompilerConfiguration(module: TestModule): CompilerConfiguration =
        getCompilerConfiguration(module, CompilationStage.FIRST)

    context(_: IrPreSerializationLoweringFacade<*>)
    fun getCompilerConfiguration(module: TestModule): CompilerConfiguration =
        getCompilerConfiguration(module, CompilationStage.FIRST)

    context(backendFacade: BackendFacade<*, *>)
    fun getCompilerConfiguration(module: TestModule): CompilerConfiguration =
        getCompilerConfiguration(module, backendFacade.outputKind.producedBy)

    context(_: DeserializerFacade<BinaryArtifacts.KLib, *>)
    fun getCompilerConfiguration(module: TestModule): CompilerConfiguration = getCompilerConfiguration(module, CompilationStage.SECOND)

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
            CompilerConfiguration()
        )
        val configuration = getCompilerConfiguration(module, CompilationStage.FIRST)
        val projectEnv = KotlinCoreEnvironment.ProjectEnvironment(testRootDisposable, applicationEnvironment, configuration)
        return KotlinCoreEnvironment.createForTests(
            projectEnv,
            configuration,
            configFiles
        ).also { registerCompilerExtensions(projectEnv.project, module, configuration) }
    }

    @OptIn(TestInfrastructureInternals::class)
    override fun getCompilerConfiguration(module: TestModule, compilationStage: CompilationStage): CompilerConfiguration {
        return configurationCache.getOrPut(module to compilationStage) { createCompilerConfiguration(module, compilationStage) }
    }

    @TestInfrastructureInternals
    fun createCompilerConfiguration(module: TestModule, compilationStage: CompilationStage): CompilerConfiguration {
        return createCompilerConfiguration(testServices, module, configurators, compilationStage).also { configuration ->
            if (testServices.cliBasedFacadesEnabled) {
                configuration.put(TEST_ONLY_PLUGIN_REGISTRATION_CALLBACK) { project ->
                    registerCompilerExtensions(project, module, configuration)
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
        else -> error("Unknown platform: ${this}")
    }
}

@TestInfrastructureInternals
fun createCompilerConfiguration(
    testServices: TestServices,
    module: TestModule,
    configurators: List<AbstractEnvironmentConfigurator>,
    compilationStage: CompilationStage,
): CompilerConfiguration {
    val configuration = CompilerConfiguration()
    configuration[CommonConfigurationKeys.MODULE_NAME] = module.name

    if (JsEnvironmentConfigurationDirectives.GENERATE_STRICT_IMPLICIT_EXPORT in module.directives) {
        configuration.generateStrictImplicitExport = true
    }

    if (JsEnvironmentConfigurationDirectives.GENERATE_DTS in module.directives) {
        configuration.generateDts = true
    }

    if (JsEnvironmentConfigurationDirectives.ES6_MODE in module.directives) {
        configuration.useEs6Classes = true
        configuration.compileSuspendAsJsGenerator = true
        configuration.compileLambdasAsEs6ArrowFunctions = JsEnvironmentConfigurationDirectives.DISABLE_ES6_ARROWS !in module.directives
        configuration.compileLongAsBigint = true
    }

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
    KlibAbiCompatibilityLevel.fromLanguageVersion(module.languageVersionSettings.languageVersion)?.let {
        configuration.klibAbiCompatibilityLevel = it
    }

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
