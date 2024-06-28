/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.fir.plugin.services.ExtensionRegistrarConfigurator
import org.jetbrains.kotlin.fir.plugin.services.PluginAnnotationsProvider
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AbstractEnvironmentConfigurator

private object CompilerPluginsDirectives : SimpleDirectivesContainer() {
    val WITH_FIR_TEST_COMPILER_PLUGIN by directive(
        description = "Configure test compiler plugin from :plugins:fir-plugin-prototype module",
        applicability = DirectiveApplicability.Global
    )
}

fun TestConfigurationBuilder.configureOptionalTestCompilerPlugin() {
    useDirectives(CompilerPluginsDirectives)

    useConfigurators(
        ::PluginAnnotationsProvider.enabledByDirective(CompilerPluginsDirectives.WITH_FIR_TEST_COMPILER_PLUGIN),
        ::ExtensionRegistrarConfigurator.enabledByDirective(CompilerPluginsDirectives.WITH_FIR_TEST_COMPILER_PLUGIN),
    )
}

private fun Constructor<AbstractEnvironmentConfigurator>.enabledByDirective(
    directive: Directive
): Constructor<AbstractEnvironmentConfigurator> {
    val originalConstructor = this
    return { testServices -> EnabledByDirectiveConfiguratorDecorator(originalConstructor(testServices), directive) }
}

private class EnabledByDirectiveConfiguratorDecorator(
    private val original: AbstractEnvironmentConfigurator,
    private val directive: Directive
) : AbstractEnvironmentConfigurator() {
    override fun configureCompileConfigurationWithAdditionalConfigurationKeys(configuration: CompilerConfiguration, module: TestModule) {
        if (directive !in module.directives) return

        original.configureCompileConfigurationWithAdditionalConfigurationKeys(configuration, module)
    }

    override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion
    ): Map<AnalysisFlag<*>, Any?> = original.provideAdditionalAnalysisFlags(directives, languageVersion)

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
