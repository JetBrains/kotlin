/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.SourceMapSourceEmbedding
import org.jetbrains.kotlin.js.config.filePathsPrefixMap
import org.jetbrains.kotlin.js.config.generatePolyfills
import org.jetbrains.kotlin.js.config.generateRegionComments
import org.jetbrains.kotlin.js.config.sourceMap
import org.jetbrains.kotlin.js.config.sourceMapEmbedSources
import org.jetbrains.kotlin.js.config.sourceMapSourceRoots
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.GENERATE_INLINE_ANONYMOUS_FUNCTIONS
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.PROPERTY_LAZY_INITIALIZATION
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.SOURCE_MAP_EMBED_SOURCES
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.DirectiveToConfigurationKeyExtractor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.targetPlatform
import java.io.File

class JsSecondStageEnvironmentConfigurator(testServices: TestServices) : JsEnvironmentConfigurator(testServices) {
    override val compilationStage: CompilationStage
        get() = CompilationStage.KLIB_TO_BINARY

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
    }
}
