/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.serialization.js.JsModuleDescriptor
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.ERROR_POLICY
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.EXPECT_ACTUAL_LINKER
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.MODULE_KIND
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.NO_INLINE
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.SOURCE_MAP_EMBED_SOURCES
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.TYPED_ARRAYS
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.util.joinToArrayString
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeAsciiOnly
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.File

class JsEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(JsEnvironmentConfigurationDirectives)

    companion object {
        const val TEST_DATA_DIR_PATH = "js/js.translator/testData"
        const val OLD_MODULE_SUFFIX = "_old"

        // Keep names short to keep path lengths under 255 for Windows
        private val outputDirByMode = mapOf(
            TranslationMode.FULL to "out",
            TranslationMode.FULL_DCE to "outMin",
            TranslationMode.PER_MODULE to "outPm",
            TranslationMode.PER_MODULE_DCE to "outPmMin"
        )

        private const val OUTPUT_KLIB_DIR_NAME = "outputKlibDir"
        private const val MINIFICATION_OUTPUT_DIR_NAME = "minOutputDir"

        object ExceptionThrowingReporter : JsConfig.Reporter() {
            override fun error(message: String) {
                throw AssertionError("Error message reported: $message")
            }
        }

        private val METADATA_CACHE = (JsConfig.JS_STDLIB + JsConfig.JS_KOTLIN_TEST).flatMap { path ->
            KotlinJavascriptMetadataUtils.loadMetadata(path).map { metadata ->
                val parts = KotlinJavascriptSerializationUtil.readModuleAsProto(metadata.body, metadata.version)
                JsModuleDescriptor(metadata.moduleName, parts.kind, parts.importedModules, parts)
            }
        }

        fun getJsArtifactSimpleName(testServices: TestServices, moduleName: String): String {
            val testName = testServices.testInfo.methodName.removePrefix("test").decapitalizeAsciiOnly()
            val outputFileSuffix = if (moduleName == ModuleStructureExtractor.DEFAULT_MODULE_NAME) "" else "-$moduleName"
            return testName + outputFileSuffix
        }

        fun getJsModuleArtifactPath(testServices: TestServices, moduleName: String, translationMode: TranslationMode = TranslationMode.FULL): String {
            return getJsArtifactsOutputDir(testServices, translationMode).absolutePath + File.separator + getJsArtifactSimpleName(testServices, moduleName) + "_v5"
        }

        fun getRecompiledJsModuleArtifactPath(testServices: TestServices, moduleName: String, translationMode: TranslationMode = TranslationMode.FULL): String {
            return getJsArtifactsRecompiledOutputDir(testServices, translationMode).absolutePath + File.separator + getJsArtifactSimpleName(testServices, moduleName) + "_v5"
        }

        fun getJsKlibArtifactPath(testServices: TestServices, moduleName: String): String {
            return getJsKlibOutputDir(testServices).absolutePath + File.separator + getJsArtifactSimpleName(testServices, moduleName)
        }

        fun getJsArtifactsOutputDir(testServices: TestServices, translationMode: TranslationMode = TranslationMode.FULL): File {
            return testServices.temporaryDirectoryManager.getOrCreateTempDirectory(outputDirByMode[translationMode]!!)
        }

        fun getJsArtifactsRecompiledOutputDir(testServices: TestServices, translationMode: TranslationMode = TranslationMode.FULL): File {
            return testServices.temporaryDirectoryManager.getOrCreateTempDirectory(outputDirByMode[translationMode]!! + "-recompiled")
        }

        fun getJsKlibOutputDir(testServices: TestServices): File {
            return testServices.temporaryDirectoryManager.getOrCreateTempDirectory(OUTPUT_KLIB_DIR_NAME)
        }

        fun getMinificationJsArtifactsOutputDir(testServices: TestServices): File {
            return testServices.temporaryDirectoryManager.getOrCreateTempDirectory(MINIFICATION_OUTPUT_DIR_NAME)
        }


        private fun getPrefixPostfixFile(module: TestModule, prefix: Boolean): File? {
            val suffix = if (prefix) ".prefix" else ".postfix"
            val originalFile = module.files.first().originalFile
            return originalFile.parentFile.resolve(originalFile.name + suffix).takeIf { it.exists() }
        }

        fun getPrefixFile(module: TestModule): File? = getPrefixPostfixFile(module, prefix = true)

        fun getPostfixFile(module: TestModule): File? = getPrefixPostfixFile(module, prefix = false)

        fun createJsConfig(
            project: Project, configuration: CompilerConfiguration, compilerEnvironment: TargetEnvironment = CompilerEnvironment
        ): JsConfig {
            return JsConfig(
                project, configuration, compilerEnvironment, METADATA_CACHE, (JsConfig.JS_STDLIB + JsConfig.JS_KOTLIN_TEST).toSet()
            )
        }

        fun getMainModule(testServices: TestServices): TestModule {
            val modules = testServices.moduleStructure.modules
            val inferMainModule = JsEnvironmentConfigurationDirectives.INFER_MAIN_MODULE in testServices.moduleStructure.allDirectives
            return when {
                inferMainModule -> modules.last()
                else -> modules.singleOrNull { it.name == ModuleStructureExtractor.DEFAULT_MODULE_NAME } ?: modules.single()
            }
        }

        fun isMainModule(module: TestModule, testServices: TestServices): Boolean {
            return module == getMainModule(testServices)
        }

        fun getMainModuleName(testServices: TestServices): String {
            return getMainModule(testServices).name
        }

        fun getRuntimePathsForModule(module: TestModule, testServices: TestServices): List<String?> {
            val result = mutableListOf<String>()
            val needsFullIrRuntime = JsEnvironmentConfigurationDirectives.KJS_WITH_FULL_RUNTIME in module.directives ||
                    ConfigurationDirectives.WITH_STDLIB in module.directives

            val names = if (needsFullIrRuntime) listOf("full.stdlib", "kotlin.test") else listOf("reduced.stdlib")
            names.mapTo(result) { System.getProperty("kotlin.js.$it.path") }
            val runtimeClasspaths = testServices.runtimeClasspathProviders.flatMap { it.runtimeClassPaths(module) }
            runtimeClasspaths.mapTo(result) { it.absolutePath }
            return result
        }

        fun getKlibDependencies(module: TestModule, testServices: TestServices, kind: DependencyRelation): List<File> {
            val visited = mutableSetOf<TestModule>()
            fun getRecursive(module: TestModule, kind: DependencyRelation) {
                val dependencies = if (kind == DependencyRelation.FriendDependency) module.friendDependencies else module.regularDependencies
                dependencies.map { testServices.dependencyProvider.getTestModule(it.moduleName) }.forEach {
                    if (it !in visited) {
                        visited += it
                        getRecursive(it, kind)
                    }
                }
            }
            getRecursive(module, kind)
            return visited.map { testServices.dependencyProvider.getArtifact(it, ArtifactKinds.KLib).outputFile }
        }

        fun getDependencies(module: TestModule, testServices: TestServices, kind: DependencyRelation): List<ModuleDescriptorImpl> {
            return getKlibDependencies(module, testServices, kind)
                .map { testServices.jsLibraryProvider.getDescriptorByPath(it.absolutePath) }
        }

        fun getMainCallParametersForModule(module: TestModule): MainCallParameters {
            return when (JsEnvironmentConfigurationDirectives.CALL_MAIN) {
                in module.directives -> MainCallParameters.mainWithArguments(listOf())
                else -> MainCallParameters.noCall()
            }
        }

        fun getAllRecursiveDependenciesFor(module: TestModule, testServices: TestServices): Set<ModuleDescriptorImpl> {
            val visited = mutableSetOf<ModuleDescriptorImpl>()
            fun getRecursive(descriptor: ModuleDescriptorImpl) {
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
            return dependencies.associateBy { testServices.jsLibraryProvider.getCompiledLibraryByDescriptor(it) }
        }

        fun TestModule.hasFilesToRecompile(): Boolean {
            return files.any { JsEnvironmentConfigurationDirectives.RECOMPILE in it.directives }
        }

        fun incrementalEnabled(testServices: TestServices): Boolean {
            return JsEnvironmentConfigurationDirectives.SKIP_IR_INCREMENTAL_CHECKS !in testServices.moduleStructure.allDirectives &&
                    testServices.moduleStructure.modules.any { it.hasFilesToRecompile() }
        }
    }


    private fun TestModule.allTransitiveDependencies(): Set<DependencyDescription> {
        val modules = testServices.moduleStructure.modules
        return regularDependencies.toSet() +
                regularDependencies.flatMap { modules.single { module -> module.name == it.moduleName }.allTransitiveDependencies() }
    }

    override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion
    ): Map<AnalysisFlag<*>, Any?> {
        return super.provideAdditionalAnalysisFlags(directives, languageVersion).toMutableMap().also {
            it[allowFullyQualifiedNameInKClass] = false
        }
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val registeredDirectives = module.directives
        val moduleKinds = registeredDirectives[MODULE_KIND]
        val moduleKind = when (moduleKinds.size) {
            0 -> testServices.moduleStructure.allDirectives[MODULE_KIND].singleOrNull()
                ?: if (JsEnvironmentConfigurationDirectives.ES_MODULES in registeredDirectives) ModuleKind.ES else ModuleKind.PLAIN
            1 -> moduleKinds.single()
            else -> error("Too many module kinds passed ${moduleKinds.joinToArrayString()}")
        }
        configuration.put(JSConfigurationKeys.MODULE_KIND, moduleKind)

        val noInline = registeredDirectives.contains(NO_INLINE)
        configuration.put(CommonConfigurationKeys.DISABLE_INLINE, noInline)

        val dependencies = module.regularDependencies.map { getJsModuleArtifactPath(testServices, it.moduleName) + ".meta.js" }
        val allDependencies = module.allTransitiveDependencies().map { getJsModuleArtifactPath(testServices, it.moduleName) + ".meta.js" }
        val friends = module.friendDependencies.map { getJsModuleArtifactPath(testServices, it.moduleName) + ".meta.js" }

        val libraries = when (module.targetBackend) {
            null -> JsConfig.JS_STDLIB + JsConfig.JS_KOTLIN_TEST
            TargetBackend.JS_IR -> dependencies
            TargetBackend.JS -> JsConfig.JS_STDLIB + JsConfig.JS_KOTLIN_TEST + dependencies
            else -> error("Unsupported target backend: ${module.targetBackend}")
        }
        configuration.put(JSConfigurationKeys.LIBRARIES, libraries)
        configuration.put(JSConfigurationKeys.TRANSITIVE_LIBRARIES, allDependencies)
        configuration.put(JSConfigurationKeys.FRIEND_PATHS, friends)

        configuration.put(CommonConfigurationKeys.MODULE_NAME, module.name.removeSuffix(OLD_MODULE_SUFFIX))
        configuration.put(JSConfigurationKeys.TARGET, EcmaVersion.v5)

        val errorIgnorancePolicy = registeredDirectives[ERROR_POLICY].singleOrNull() ?: ErrorTolerancePolicy.DEFAULT
        configuration.put(JSConfigurationKeys.ERROR_TOLERANCE_POLICY, errorIgnorancePolicy)
        if (errorIgnorancePolicy.allowErrors) {
            configuration.put(JSConfigurationKeys.DEVELOPER_MODE, true)
        }
        if (errorIgnorancePolicy != ErrorTolerancePolicy.DEFAULT) {
            configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        }

        val multiModule = testServices.moduleStructure.modules.size > 1
        configuration.put(JSConfigurationKeys.META_INFO, multiModule)

        val sourceDirs = module.files.map { it.originalFile.parent }.distinct()
        configuration.put(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, sourceDirs)
        configuration.put(JSConfigurationKeys.SOURCE_MAP, true)

        val sourceMapSourceEmbedding = registeredDirectives[SOURCE_MAP_EMBED_SOURCES].singleOrNull() ?: SourceMapSourceEmbedding.NEVER
        configuration.put(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES, sourceMapSourceEmbedding)

        configuration.put(JSConfigurationKeys.TYPED_ARRAYS_ENABLED, TYPED_ARRAYS in registeredDirectives)

        configuration.put(JSConfigurationKeys.GENERATE_REGION_COMMENTS, true)

        configuration.put(
            JSConfigurationKeys.FILE_PATHS_PREFIX_MAP,
            mapOf(File(".").absolutePath.removeSuffix(".") to "")
        )

        configuration.put(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER, EXPECT_ACTUAL_LINKER in registeredDirectives)
    }
}

