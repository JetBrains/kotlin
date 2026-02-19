/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.evaluatedConstTracker
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.GENERATE_INLINE_ANONYMOUS_FUNCTIONS
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.PROPERTY_LAZY_INITIALIZATION
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.SOURCE_MAP_EMBED_SOURCES
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import java.io.File

open class JsSecondStageEnvironmentConfigurator(testServices: TestServices) : JsEnvironmentConfigurator(testServices) {
    override val compilationStage: CompilationStage
        get() = CompilationStage.SECOND

    override fun DirectiveToConfigurationKeyExtractor.provideConfigurationKeys() {
        register(PROPERTY_LAZY_INITIALIZATION, JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION)
        register(GENERATE_INLINE_ANONYMOUS_FUNCTIONS, JSConfigurationKeys.GENERATE_INLINE_ANONYMOUS_FUNCTIONS)
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (!module.targetPlatform(testServices).isJs()) return

        super.configureCompilerConfiguration(configuration, module)

        val sourceDirs = module.files.mapNotNull { it.originalFile.parent }.distinct()
        configuration.sourceMapSourceRoots = sourceDirs
        configuration.sourceMap = true

        val sourceMapSourceEmbedding = module.directives[SOURCE_MAP_EMBED_SOURCES].singleOrNull() ?: SourceMapSourceEmbedding.NEVER
        configuration.sourceMapEmbedSources = sourceMapSourceEmbedding

        configuration.generatePolyfills = true
        configuration.generateRegionComments = true

        configuration.filePathsPrefixMap = mapOf(File(".").absolutePath.removeSuffix(".") to "")

        val firstPhaseConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module, CompilationStage.FIRST)
        configuration.putIfAbsent(
            CommonConfigurationKeys.EVALUATED_CONST_TRACKER,
            firstPhaseConfiguration.evaluatedConstTracker ?: EvaluatedConstTracker.create()
        )
    }
}
