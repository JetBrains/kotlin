/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.SourceMapSourceEmbedding
import org.jetbrains.kotlin.js.config.WasmTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.INFER_MAIN_MODULE
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.EXPECT_ACTUAL_LINKER
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.PROPERTY_LAZY_INITIALIZATION
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.SOURCE_MAP_EMBED_SOURCES
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.DISABLE_WASM_EXCEPTION_HANDLING
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import java.io.File

class WasmEnvironmentConfiguratorJs(testServices: TestServices) : WasmEnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        super.configureCompilerConfiguration(configuration, module)
        configuration.put(JSConfigurationKeys.WASM_TARGET, WasmTarget.JS)
    }
}

class WasmEnvironmentConfiguratorWasi(testServices: TestServices) : WasmEnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        super.configureCompilerConfiguration(configuration, module)
        configuration.put(JSConfigurationKeys.WASM_TARGET, WasmTarget.WASI)
    }
}

abstract class WasmEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(WasmEnvironmentConfigurationDirectives)

    companion object {
        private const val OUTPUT_KLIB_DIR_NAME = "outputKlibDir"

        fun getRuntimePathsForModule(target: WasmTarget): List<String> {
            val suffix = when (target) {
                WasmTarget.JS -> "-js"
                WasmTarget.WASI -> "-wasi"
                else -> error("Unexpected wasi target")
            }
            return listOf(System.getProperty("kotlin.wasm$suffix.stdlib.path")!!, System.getProperty("kotlin.wasm$suffix.kotlin.test.path")!!)
        }

        fun getKlibDependencies(module: TestModule, testServices: TestServices, kind: DependencyRelation): List<File> {
            val visited = mutableSetOf<TestModule>()
            fun getRecursive(module: TestModule, relation: DependencyRelation) {
                val dependencies = if (relation == DependencyRelation.FriendDependency) {
                    module.friendDependencies
                } else {
                    module.regularDependencies
                }
                dependencies
                    .filter { it.kind != DependencyKind.Source }
                    .map { testServices.dependencyProvider.getTestModule(it.moduleName) }.forEach {
                        if (it !in visited) {
                            visited += it
                            getRecursive(it, relation)
                        }
                    }
            }
            getRecursive(module, kind)
            return visited.map { testServices.dependencyProvider.getArtifact(it, ArtifactKinds.KLib).outputFile }
        }

        fun getDependencies(module: TestModule, testServices: TestServices, kind: DependencyRelation): List<ModuleDescriptor> {
            return getKlibDependencies(module, testServices, kind)
                .map { testServices.libraryProvider.getDescriptorByPath(it.absolutePath) }
        }


        fun getWasmKlibArtifactPath(testServices: TestServices, moduleName: String): String {
            return getWasmKlibOutputDir(testServices).absolutePath + File.separator + JsEnvironmentConfigurator.getJsArtifactSimpleName(
                testServices,
                moduleName
            )
        }

        fun getWasmKlibOutputDir(testServices: TestServices): File {
            return testServices.temporaryDirectoryManager.getOrCreateTempDirectory(OUTPUT_KLIB_DIR_NAME)
        }

        fun getAllRecursiveDependenciesFor(module: TestModule, testServices: TestServices): Set<ModuleDescriptorImpl> {
            val visited = mutableSetOf<ModuleDescriptorImpl>()
            fun getRecursive(descriptor: ModuleDescriptor) {
                descriptor.allDependencyModules.forEach {
                    if (it is ModuleDescriptorImpl && it !in visited) {
                        visited += it
                        getRecursive(it)
                    }
                }
            }

            getRecursive(testServices.moduleDescriptorProvider.getModuleDescriptor(module))
            return visited
        }

        fun getAllRecursiveLibrariesFor(module: TestModule, testServices: TestServices): Map<KotlinLibrary, ModuleDescriptorImpl> {
            val dependencies = getAllRecursiveDependenciesFor(module, testServices)
            return dependencies.associateBy { testServices.libraryProvider.getCompiledLibraryByDescriptor(it) }
        }

        fun getAllDependenciesMappingFor(module: TestModule, testServices: TestServices): Map<KotlinLibrary, List<KotlinLibrary>> {
            val allRecursiveLibraries: Map<KotlinLibrary, ModuleDescriptor> =
                getAllRecursiveLibrariesFor(module, testServices)
            val m2l = allRecursiveLibraries.map { it.value to it.key }.toMap()

            return allRecursiveLibraries.keys.associateWith { m ->
                val descriptor = allRecursiveLibraries[m] ?: error("No descriptor found for library ${m.libraryName}")
                descriptor.allDependencyModules.filter { it != descriptor }.map { m2l.getValue(it) }
            }
        }

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
        languageVersion: LanguageVersion
    ): Map<AnalysisFlag<*>, Any?> {
        return super.provideAdditionalAnalysisFlags(directives, languageVersion).toMutableMap().also {
            it[allowFullyQualifiedNameInKClass] = false
        }
    }

    override fun DirectiveToConfigurationKeyExtractor.provideConfigurationKeys() {
        register(PROPERTY_LAZY_INITIALIZATION, JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION)
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val registeredDirectives = module.directives
        configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.ES)
        configuration.put(CommonConfigurationKeys.MODULE_NAME, module.name)

        configuration.put(JSConfigurationKeys.WASM_ENABLE_ASSERTS, true)
        configuration.put(JSConfigurationKeys.WASM_ENABLE_ARRAY_RANGE_CHECKS, true)

        val sourceDirs = module.files.map { it.originalFile.parent }.distinct()
        configuration.put(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, sourceDirs)
        configuration.put(JSConfigurationKeys.SOURCE_MAP, true)

        val sourceMapSourceEmbedding = registeredDirectives[SOURCE_MAP_EMBED_SOURCES].singleOrNull() ?: SourceMapSourceEmbedding.NEVER
        configuration.put(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES, sourceMapSourceEmbedding)

        configuration.put(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER, EXPECT_ACTUAL_LINKER in registeredDirectives)

        configuration.put(JSConfigurationKeys.WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS, DISABLE_WASM_EXCEPTION_HANDLING in registeredDirectives)

        configuration.put(JSConfigurationKeys.WASM_TARGET, WasmTarget.JS)
    }
}