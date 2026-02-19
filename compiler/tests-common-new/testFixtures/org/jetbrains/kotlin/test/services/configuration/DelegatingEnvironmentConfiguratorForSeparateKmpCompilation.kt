/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

abstract class DelegatingEnvironmentConfiguratorForSeparateKmpCompilation(
    val testServices: TestServices,
    delegate: Constructor<EnvironmentConfigurator>
) : AbstractEnvironmentConfigurator() {
    private val delegate = delegate(testServices)

    protected abstract fun shouldApply(module: TestModule): Boolean

    override val additionalServices: List<ServiceRegistrationData>
        get() = delegate.additionalServices

    override val directiveContainers: List<DirectivesContainer>
        get() = delegate.directiveContainers

    override val compilationStage: CompilationStage
        get() = delegate.compilationStage

    final override fun configureCompileConfigurationWithAdditionalConfigurationKeys(configuration: CompilerConfiguration, module: TestModule) {
        require(ConfigurationDirectives.SEPARATE_KMP_COMPILATION in module.directives) {
            "SEPARATE_KMP_COMPILATION should be enabled"
        }
        if (!shouldApply(module)) return
        delegate.configureCompileConfigurationWithAdditionalConfigurationKeys(configuration, module)
    }

    final override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion,
    ): Map<AnalysisFlag<*>, Any?> {
        return delegate.provideAdditionalAnalysisFlags(directives, languageVersion)
    }

    final override fun legacyRegisterCompilerExtensions(
        project: Project,
        module: TestModule,
        configuration: CompilerConfiguration,
    ) {
        if (!shouldApply(module)) return
        return delegate.legacyRegisterCompilerExtensions(project, module, configuration)
    }

    final override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration,
    ) {
        if (!shouldApply(module)) return
        with(delegate) {
            registerCompilerExtensions(module, configuration)
        }
    }
}

class MetadataEnvironmentConfiguratorForSeparateKmpCompilation(
    testServices: TestServices
) : DelegatingEnvironmentConfiguratorForSeparateKmpCompilation(testServices, ::MetadataEnvironmentConfigurator) {
    override fun shouldApply(module: TestModule): Boolean {
        return !module.isLeafModuleInMppGraph(testServices)
    }
}

class JvmEnvironmentConfiguratorForSeparateKmpCompilation(
    testServices: TestServices
) : DelegatingEnvironmentConfiguratorForSeparateKmpCompilation(testServices, ::JvmEnvironmentConfigurator) {
    override fun shouldApply(module: TestModule): Boolean {
        return module.isLeafModuleInMppGraph(testServices)
    }
}
