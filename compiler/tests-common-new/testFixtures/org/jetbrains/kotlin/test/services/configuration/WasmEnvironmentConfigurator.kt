/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.INFER_MAIN_MODULE
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.PROPERTY_LAZY_INITIALIZATION
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.SOURCE_MAP_EMBED_SOURCES
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.DISABLE_WASM_EXCEPTION_HANDLING
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.FORCE_DEBUG_FRIENDLY_COMPILATION
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_NEW_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_OLD_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.WASM_DISABLE_FQNAME_IN_KCLASS
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.WASM_NO_JS_TAG
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys

abstract class WasmEnvironmentConfigurator(
    testServices: TestServices,
    protected val wasmTarget: WasmTarget,
) : EnvironmentConfigurator(testServices), KlibBasedEnvironmentConfigurator {

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(WasmEnvironmentConfigurationDirectives, KlibBasedCompilerTestDirectives)

    companion object {
        fun getRuntimePathsForModule(target: WasmTarget, testServices: TestServices): List<String> {
            return listOf(stdlibPath(target, testServices), kotlinTestPath(target, testServices))
        }

        fun kotlinTestPath(target: WasmTarget): String = System.getProperty("kotlin.${target.alias}.kotlin.test.path")!!
        fun stdlibPath(target: WasmTarget): String = System.getProperty("kotlin.${target.alias}.stdlib.path")!!

        fun kotlinTestPath(target: WasmTarget, testServices: TestServices): String =
            testServices.standardLibrariesPathProvider.fullWasmStdlib(target).absolutePath

        fun stdlibPath(target: WasmTarget, testServices: TestServices): String =
            testServices.standardLibrariesPathProvider.kotlinTestWasmKLib(target).absolutePath

        fun getMainModule(testServices: TestServices): TestModule {
            val modules = testServices.moduleStructure.modules
            val inferMainModule = INFER_MAIN_MODULE in testServices.moduleStructure.allDirectives
            return when {
                inferMainModule -> modules.last()
                else -> modules.singleOrNull { it.name == ModuleStructureExtractor.DEFAULT_MODULE_NAME } ?: modules.last()
            }
        }

        fun isMainModule(module: TestModule, testServices: TestServices): Boolean {
            return module == getMainModule(testServices)
        }
    }


    override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion,
    ): Map<AnalysisFlag<*>, Any?> {
        return super.provideAdditionalAnalysisFlags(directives, languageVersion).toMutableMap().also {
            it[allowFullyQualifiedNameInKClass] = WASM_DISABLE_FQNAME_IN_KCLASS !in directives
        }
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.moduleKind = ModuleKind.ES
        configuration.moduleName = module.name

        configuration.put(JSConfigurationKeys.WASM_COMPILATION, true)
        configuration.put(WasmConfigurationKeys.WASM_TARGET, wasmTarget)
        configuration.put(WasmConfigurationKeys.WASM_COMMAND_MODULE, wasmTarget == WasmTarget.WASI)
    }
}

class WasmFirstStageEnvironmentConfigurator(
    testServices: TestServices,
    wasmTarget: WasmTarget,
) : WasmEnvironmentConfigurator(testServices, wasmTarget) {
    override val compilationStage: CompilationStage
        get() = CompilationStage.FIRST

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        super.configureCompilerConfiguration(configuration, module)

        configuration.outputDir = getKlibArtifactFile(testServices, module.name)

        val dependencies = module.regularDependencies.map { getKlibArtifactFile(testServices, it.dependencyModule.name).absolutePath }
        val friends = module.friendDependencies.map { getKlibArtifactFile(testServices, it.dependencyModule.name).absolutePath }
        val libraries = getRuntimePathsForModule(wasmTarget, testServices) + dependencies + friends

        configuration.libraries = libraries
        configuration.friendLibraries = friends
    }
}

open class WasmSecondStageEnvironmentConfigurator(
    testServices: TestServices,
    wasmTarget: WasmTarget,
) : WasmEnvironmentConfigurator(testServices, wasmTarget) {
    override val compilationStage: CompilationStage
        get() = CompilationStage.SECOND

    override fun DirectiveToConfigurationKeyExtractor.provideConfigurationKeys() {
        register(PROPERTY_LAZY_INITIALIZATION, JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION)
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        super.configureCompilerConfiguration(configuration, module)
        val registeredDirectives = module.directives

        configuration.put(WasmConfigurationKeys.WASM_ENABLE_ASSERTS, true)
        configuration.put(WasmConfigurationKeys.WASM_ENABLE_ARRAY_RANGE_CHECKS, true)

        val sourceDirs = module.files.map { it.originalFile.parent }.distinct()
        configuration.sourceMapSourceRoots = sourceDirs
        configuration.sourceMap = true
        configuration.sourceMapIncludeMappingsFromUnavailableFiles =
            SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES in registeredDirectives

        val sourceMapSourceEmbedding = registeredDirectives[SOURCE_MAP_EMBED_SOURCES].singleOrNull() ?: SourceMapSourceEmbedding.NEVER
        configuration.put(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES, sourceMapSourceEmbedding)

        configuration.put(WasmConfigurationKeys.WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS, DISABLE_WASM_EXCEPTION_HANDLING in registeredDirectives)

        val hasUseNewExceptionsDirective = USE_NEW_EXCEPTION_HANDLING_PROPOSAL in registeredDirectives
        val hasUseOldExceptionsDirective = USE_OLD_EXCEPTION_HANDLING_PROPOSAL in registeredDirectives
        if (hasUseNewExceptionsDirective && hasUseOldExceptionsDirective) error("Can't use both old and new exception handling proposals")

        val useNewExceptions = when {
            hasUseNewExceptionsDirective -> true
            hasUseOldExceptionsDirective -> false
            else -> wasmTarget == WasmTarget.WASI
        }

        configuration.put(WasmConfigurationKeys.WASM_USE_NEW_EXCEPTION_PROPOSAL, useNewExceptions)
        configuration.put(WasmConfigurationKeys.WASM_NO_JS_TAG, WASM_NO_JS_TAG in registeredDirectives)
        configuration.put(
            WasmConfigurationKeys.WASM_INTERNAL_LOCAL_VARIABLE_PREFIX,
            K2JSCompilerArguments().wasmInternalLocalVariablePrefix
        )
        configuration.put(
            WasmConfigurationKeys.WASM_FORCE_DEBUG_FRIENDLY_COMPILATION,
            FORCE_DEBUG_FRIENDLY_COMPILATION in registeredDirectives
        )

        val firstPhaseConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module, CompilationStage.FIRST)
        configuration.putIfAbsent(
            CommonConfigurationKeys.EVALUATED_CONST_TRACKER,
            firstPhaseConfiguration.evaluatedConstTracker ?: EvaluatedConstTracker.create()
        )
    }
}

private class WasmJsCompilerConfigurationKeyEnablerConfigurator(
    testServices: TestServices,
    private val wasmConfigurationKey: CompilerConfigurationKey<Boolean>
) : WasmSecondStageEnvironmentConfigurator(testServices, WasmTarget.JS) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        super.configureCompilerConfiguration(configuration, module)
        configuration.put(wasmConfigurationKey, true)
    }
}

fun TestConfigurationBuilder.enableByConfigurationKey(key: CompilerConfigurationKey<Boolean>) {
    useConfigurators(
        { WasmJsCompilerConfigurationKeyEnablerConfigurator(it, key) }
    )
}