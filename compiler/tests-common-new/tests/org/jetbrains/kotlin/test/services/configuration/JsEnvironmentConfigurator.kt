/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.SourceMapSourceEmbedding
import org.jetbrains.kotlin.js.config.friendLibraries
import org.jetbrains.kotlin.js.config.outputDir
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.GENERATE_INLINE_ANONYMOUS_FUNCTIONS
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.MODULE_KIND
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.NO_INLINE
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.PROPERTY_LAZY_INITIALIZATION
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.SOURCE_MAP_EMBED_SOURCES
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.KLIB_RELATIVE_PATH_BASES
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.util.joinToArrayString
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import java.io.File

class JsEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(JsEnvironmentConfigurationDirectives, KlibBasedCompilerTestDirectives)

    companion object : KlibBasedEnvironmentConfiguratorUtils {
        const val TEST_DATA_DIR_PATH = "js/js.translator/testData"
        const val OLD_MODULE_SUFFIX = "_old"

        // Keep names short to keep path lengths under 255 for Windows
        private val outputDirByMode = mapOf(
            TranslationMode.FULL_DEV to "out",
            TranslationMode.FULL_PROD_MINIMIZED_NAMES to "outMin",
            TranslationMode.PER_MODULE_DEV to "outPm",
            TranslationMode.PER_MODULE_PROD_MINIMIZED_NAMES to "outPmMin",
            TranslationMode.PER_FILE_DEV to "outPf",
            TranslationMode.PER_FILE_PROD_MINIMIZED_NAMES to "outPfMin"
        )

        private const val MINIFICATION_OUTPUT_DIR_NAME = "minOutputDir"

        fun getJsModuleArtifactPath(testServices: TestServices, moduleName: String, translationMode: TranslationMode = TranslationMode.FULL_DEV): String {
            return getJsArtifactsOutputDir(testServices, translationMode).absolutePath + File.separator + getJsModuleArtifactName(testServices, moduleName)
        }

        fun getRecompiledJsModuleArtifactPath(testServices: TestServices, moduleName: String, translationMode: TranslationMode = TranslationMode.FULL_DEV): String {
            return getJsArtifactsRecompiledOutputDir(testServices, translationMode).absolutePath + File.separator + getJsModuleArtifactName(testServices, moduleName)
        }

        fun getJsModuleArtifactName(testServices: TestServices, moduleName: String): String {
            return getKlibArtifactSimpleName(testServices, moduleName) + "_v5"
        }

        fun getJsArtifactsOutputDir(testServices: TestServices, translationMode: TranslationMode = TranslationMode.FULL_DEV): File {
            return testServices.temporaryDirectoryManager.getOrCreateTempDirectory(outputDirByMode[translationMode]!!)
        }

        fun getJsArtifactsRecompiledOutputDir(testServices: TestServices, translationMode: TranslationMode = TranslationMode.FULL_DEV): File {
            return testServices.temporaryDirectoryManager.getOrCreateTempDirectory(outputDirByMode[translationMode]!! + "-recompiled")
        }

        fun getMinificationJsArtifactsOutputDir(testServices: TestServices): File {
            return testServices.temporaryDirectoryManager.getOrCreateTempDirectory(MINIFICATION_OUTPUT_DIR_NAME)
        }


        fun getMainModule(testServices: TestServices): TestModule {
            val modules = testServices.moduleStructure.modules
            val inferMainModule = JsEnvironmentConfigurationDirectives.INFER_MAIN_MODULE in testServices.moduleStructure.allDirectives
            return when {
                inferMainModule -> modules.last()
                else -> modules.singleOrNull { it.name == ModuleStructureExtractor.DEFAULT_MODULE_NAME } ?: modules.last()
            }
        }

        fun isMainModule(module: TestModule, testServices: TestServices): Boolean {
            return module == getMainModule(testServices)
        }

        fun getMainModuleName(testServices: TestServices): String {
            return getMainModule(testServices).name
        }

        fun getRuntimePathsForModule(module: TestModule, testServices: TestServices): List<String> {
            val result = mutableListOf<String>()
            val needsFullIrRuntime = JsEnvironmentConfigurationDirectives.KJS_WITH_FULL_RUNTIME in module.directives ||
                    ConfigurationDirectives.WITH_STDLIB in module.directives

            val pathProvider = testServices.standardLibrariesPathProvider
            if (needsFullIrRuntime) {
                result += pathProvider.fullJsStdlib().absolutePath
                result += pathProvider.kotlinTestJsKLib().absolutePath
            } else {
                result += pathProvider.defaultJsStdlib().absolutePath
            }
            val runtimeClasspaths = testServices.runtimeClasspathProviders.flatMap { it.runtimeClassPaths(module) }
            runtimeClasspaths.mapTo(result) { it.absolutePath }
            return result
        }

        fun getMainCallParametersForModule(module: TestModule): List<String>? {
            return when {
                JsEnvironmentConfigurationDirectives.CALL_MAIN in module.directives -> listOf()
                JsEnvironmentConfigurationDirectives.MAIN_ARGS in module.directives -> {
                    module.directives[JsEnvironmentConfigurationDirectives.MAIN_ARGS].single()
                }
                else -> null
            }
        }

        fun TestModule.hasFilesToRecompile(): Boolean {
            return files.any { JsEnvironmentConfigurationDirectives.RECOMPILE in it.directives }
        }

        fun incrementalEnabled(testServices: TestServices): Boolean {
            return JsEnvironmentConfigurationDirectives.SKIP_IR_INCREMENTAL_CHECKS !in testServices.moduleStructure.allDirectives &&
                    testServices.moduleStructure.modules.any { it.hasFilesToRecompile() }
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
        register(GENERATE_INLINE_ANONYMOUS_FUNCTIONS, JSConfigurationKeys.GENERATE_INLINE_ANONYMOUS_FUNCTIONS)
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (!module.targetPlatform(testServices).isJs()) return

        configuration.phaseConfig = createJsTestPhaseConfig(testServices, module)
        configuration.outputDir = getKlibArtifactFile(testServices, module.name)

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

        val dependencies = module.regularDependencies.map { getKlibArtifactFile(testServices, it.dependencyModule.name).absolutePath }
        val friends = module.friendDependencies.map { getKlibArtifactFile(testServices, it.dependencyModule.name).absolutePath }

        val libraries = when (val targetBackend = testServices.defaultsProvider.targetBackend) {
            null -> listOf(
                testServices.standardLibrariesPathProvider.fullJsStdlib().absolutePath,
                testServices.standardLibrariesPathProvider.kotlinTestJsKLib().absolutePath
            )
            TargetBackend.JS_IR, TargetBackend.JS_IR_ES6 -> getRuntimePathsForModule(module, testServices) + dependencies + friends
            else -> error("Unsupported target backend: $targetBackend")
        }
        configuration.put(JSConfigurationKeys.LIBRARIES, libraries)
        configuration.friendLibraries = friends

        configuration.put(CommonConfigurationKeys.MODULE_NAME, module.name.removeSuffix(OLD_MODULE_SUFFIX))
        configuration.put(JSConfigurationKeys.TARGET, EcmaVersion.es5)

        val multiModule = testServices.moduleStructure.modules.size > 1
        configuration.put(JSConfigurationKeys.META_INFO, multiModule)

        val sourceDirs = module.files.mapNotNull { it.originalFile.parent }.distinct()
        configuration.put(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, sourceDirs)
        configuration.put(JSConfigurationKeys.SOURCE_MAP, true)

        val sourceMapSourceEmbedding = registeredDirectives[SOURCE_MAP_EMBED_SOURCES].singleOrNull() ?: SourceMapSourceEmbedding.NEVER
        configuration.put(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES, sourceMapSourceEmbedding)

        configuration.put(JSConfigurationKeys.GENERATE_POLYFILLS, true)
        configuration.put(JSConfigurationKeys.GENERATE_REGION_COMMENTS, true)

        configuration.put(
            JSConfigurationKeys.FILE_PATHS_PREFIX_MAP,
            mapOf(File(".").absolutePath.removeSuffix(".") to "")
        )

        configuration.klibRelativePathBases = registeredDirectives[KLIB_RELATIVE_PATH_BASES].applyIf(testServices.cliBasedFacadesEnabled) {
            val modulePath = testServices.sourceFileProvider.getKotlinSourceDirectoryForModule(module).canonicalPath
            map { "$modulePath/$it" }
        }

        if (testServices.cliBasedFacadesEnabled) {
            configuration.addSourcesForDependsOnClosure(module, testServices)
        }
    }
}

