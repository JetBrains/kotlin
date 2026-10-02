/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.test.framework.directives.CompilerPluginsDirectives
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.plugin.sandbox.ExtensionRegistrarConfigurator
import org.jetbrains.kotlin.plugin.sandbox.PluginAnnotationsProvider
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AbstractEnvironmentConfigurator

/**
 * Registers the test compiler plugin from ':plugins:plugin-sandbox' for modules with the
 * [WITH_FIR_TEST_COMPILER_PLUGIN][CompilerPluginsDirectives.WITH_FIR_TEST_COMPILER_PLUGIN] directive.
 *
 * The plugin is not a part of the published test framework, so its classes are only loaded once a module uses the directive.
 */
fun TestConfigurationBuilder.configureOptionalTestCompilerPlugin() {
    useDirectives(CompilerPluginsDirectives)

    val directive = CompilerPluginsDirectives.WITH_FIR_TEST_COMPILER_PLUGIN

    useConfigurators(
        { testServices -> EnabledByDirectiveConfiguratorDecorator(directive) { PluginAnnotationsProvider(testServices) } },
        { testServices -> EnabledByDirectiveConfiguratorDecorator(directive) { ExtensionRegistrarConfigurator(testServices) } },
    )
}

/**
 * Delegates to the configurator created by [originalFactory] only for modules with the [directive].
 * The configurator is created on the first such module.
 */
private class EnabledByDirectiveConfiguratorDecorator(
    private val directive: Directive,
    originalFactory: () -> AbstractEnvironmentConfigurator,
) : AbstractEnvironmentConfigurator() {
    private val original: AbstractEnvironmentConfigurator by lazy(originalFactory)

    override fun configureCompileConfigurationWithAdditionalConfigurationKeys(configuration: CompilerConfiguration, module: TestModule) {
        if (directive !in module.directives) return

        original.configureCompileConfigurationWithAdditionalConfigurationKeys(configuration, module)
    }

    override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion
    ): Map<AnalysisFlag<*>, Any?> {
        if (directive !in directives) return emptyMap()

        return original.provideAdditionalAnalysisFlags(directives, languageVersion)
    }

    override fun legacyRegisterCompilerExtensions(project: Project, module: TestModule, configuration: CompilerConfiguration) {
        if (directive !in module.directives) return

        original.legacyRegisterCompilerExtensions(project, module, configuration)
    }

    @OptIn(ExperimentalCompilerApi::class)
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        if (directive !in module.directives) return

        with(original) {
            this@registerCompilerExtensions.registerCompilerExtensions(module, configuration)
        }
    }
}
