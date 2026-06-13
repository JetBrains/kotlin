/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.backend.common.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.PartialLinkageConfig
import org.jetbrains.kotlin.config.PartialLinkageLogLevel
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.CALL_MAIN
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.DELEGATE_JS_TRANSPILATION
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.DISABLE_ES6_ARROWS
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.ES6_MODE
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.GENERATE_INLINE_ANONYMOUS_FUNCTIONS
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.GENERATE_STRICT_IMPLICIT_EXPORT
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.KEEP
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.PROPERTY_LAZY_INITIALIZATION
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.SAFE_EXTERNAL_BOOLEAN
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.SOURCE_MAP_EMBED_SOURCES
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.TS_COMPILATION_STRATEGY
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import java.io.File

open class JsSecondStageEnvironmentConfigurator(testServices: TestServices) : JsEnvironmentConfigurator(testServices) {
    override val compilationStage: CompilationStage
        get() = CompilationStage.SECOND

    companion object {
        fun getArtifactConfigurations(
            testServices: TestServices,
            module: TestModule,
            configuration: CompilerConfiguration,
            firstTimeCompilation: Boolean,
        ): List<WebArtifactConfiguration> {
            val moduleKind = configuration.moduleKind ?: error("Missing module kind")
            val translationModes = if (incrementalEnabled(testServices)) {
                listOf(TranslationMode.FULL_DEV, TranslationMode.PER_MODULE_DEV)
            } else {
                getTranslationModesForTest(testServices, module)
            }
            return translationModes.map { mode ->
                val outputFile = File(
                    getJsModuleArtifactPath(testServices, module.name, mode, firstTimeCompilation)
                        .finalizePath(moduleKind)
                )
                val rootDir = outputFile.parentFile

                // CompilationOutputs keeps the `outputDir` clean by removing all outdated JS and other unknown files.
                // To ensure that useful files around `outputFile`, such as irdump, are not removed, use `tmpBuildDir` instead.
                val tmpBuildDir = rootDir.resolve("tmp-build")

                WebArtifactConfiguration(
                    moduleName = configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME),
                    moduleKind = moduleKind,
                    outputDirectory = tmpBuildDir,
                    outputName = outputFile.nameWithoutExtension,
                    granularity = mode.granularity,
                    tsCompilationStrategy = module.directives[TS_COMPILATION_STRATEGY].lastOrNull() ?: TsCompilationStrategy.NONE,
                    production = mode.production,
                    minimizedMemberNames = mode.minimizedMemberNames,
                )
            }
        }
    }

    override fun DirectiveToConfigurationKeyExtractor.provideConfigurationKeys() {
        register(PROPERTY_LAZY_INITIALIZATION, JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION)
        register(GENERATE_INLINE_ANONYMOUS_FUNCTIONS, JSConfigurationKeys.GENERATE_INLINE_ANONYMOUS_FUNCTIONS)
        register(CALL_MAIN, JSConfigurationKeys.CALL_MAIN)
        register(SAFE_EXTERNAL_BOOLEAN, JSConfigurationKeys.SAFE_EXTERNAL_BOOLEAN)
        register(SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC, JSConfigurationKeys.SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC)
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (!module.targetPlatform(testServices).isJs()) return

        super.configureCompilerConfiguration(configuration, module)

        val runtimeKlibs = getRuntimePathsForModule(module, testServices)
        val klibDependencies = getKlibDependencies(module, testServices, DependencyRelation.RegularDependency)
            .map { it.absolutePath }
        val klibFriendDependencies = getKlibDependencies(module, testServices, DependencyRelation.FriendDependency)
            .map { it.absolutePath }

        val klibArtifact = testServices.artifactsProvider.getArtifact(module, ArtifactKinds.KLib)
        val mainModule = MainModule.Klib(klibArtifact.outputFile.absolutePath)
        val mainPath = File(mainModule.libPath).canonicalPath
        configuration.libraries = runtimeKlibs + klibDependencies + klibFriendDependencies + mainPath
        configuration.friendLibraries = klibFriendDependencies
        configuration.includes = mainPath

        val sourceDirs = module.files.mapNotNull { it.originalFile.parent }.distinct()
        configuration.sourceMapSourceRoots = sourceDirs
        configuration.sourceMap = true

        val sourceMapSourceEmbedding = module.directives[SOURCE_MAP_EMBED_SOURCES].singleOrNull() ?: SourceMapSourceEmbedding.NEVER
        configuration.sourceMapEmbedSources = sourceMapSourceEmbedding

        configuration.generatePolyfills = true
        configuration.generateRegionComments = true

        configuration.filePathsPrefixMap = mapOf(File(".").absolutePath.removeSuffix(".") to "")

        // Enforce PL with the ERROR log level to fail any tests where PL detected any incompatibilities.
        configuration.setupPartialLinkageConfig(PartialLinkageConfig(PartialLinkageLogLevel.ERROR))

        configuration.keep = module.directives[KEEP]

        val testPackage = extractTestPackage(testServices, ignoreEsModules = false)
        configuration.additionalExportedDeclarationNames = setOf(testPackage.child(Name.identifier("box")))
        configuration.artifactConfigurations = getArtifactConfigurations(testServices, module, configuration, firstTimeCompilation = true)

        if (GENERATE_STRICT_IMPLICIT_EXPORT in module.directives) {
            configuration.generateStrictImplicitExport = true
        }
        if ((module.directives[TS_COMPILATION_STRATEGY].lastOrNull() ?: TsCompilationStrategy.NONE) != TsCompilationStrategy.NONE) {
            configuration.generateDts = true
        }
        if (ES6_MODE in module.directives || DELEGATE_JS_TRANSPILATION in module.directives) {
            configuration.useEs6Classes = true
            configuration.compileSuspendAsJsGenerator = true
            configuration.compileLambdasAsEs6ArrowFunctions = DISABLE_ES6_ARROWS !in module.directives
            configuration.compileLongAsBigint = true
        }
    }
}
