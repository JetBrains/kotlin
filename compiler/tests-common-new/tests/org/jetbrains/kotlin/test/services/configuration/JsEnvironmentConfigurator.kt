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
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.serialization.js.JsModuleDescriptor
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.ERROR_POLICY
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.GENERATE_INLINE_ANONYMOUS_FUNCTIONS
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.MODULE_KIND
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.NO_INLINE
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.PROPERTY_LAZY_INITIALIZATION
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.SOURCE_MAP_EMBED_SOURCES
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.TYPED_ARRAYS
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.util.joinToArrayString
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.File

class JsEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(JsEnvironmentConfigurationDirectives)

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

        object ExceptionThrowingReporter : JsConfig.Reporter() {
            override fun error(message: String) {
                throw AssertionError("Error message reported: $message")
            }
        }

        private val METADATA_CACHE by lazy {
            (JsConfig.JS_STDLIB + JsConfig.JS_KOTLIN_TEST).flatMap { path ->
                KotlinJavascriptMetadataUtils.loadMetadata(path).map { metadata ->
                    val parts = KotlinJavascriptSerializationUtil.readModuleAsProto(metadata.body, metadata.version)
                    JsModuleDescriptor(metadata.moduleName, parts.kind, parts.importedModules, parts)
                }
            }
        }

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

        fun getMainCallParametersForModule(module: TestModule): MainCallParameters {
            return when {
                JsEnvironmentConfigurationDirectives.CALL_MAIN in module.directives -> MainCallParameters.mainWithArguments(listOf())
                JsEnvironmentConfigurationDirectives.MAIN_ARGS in module.directives -> {
                    MainCallParameters.mainWithArguments(module.directives[JsEnvironmentConfigurationDirectives.MAIN_ARGS].single())
                }
                else -> MainCallParameters.noCall()
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
        if (module.targetPlatform !in JsPlatforms.allJsPlatforms) return

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
            TargetBackend.JS_IR, TargetBackend.JS_IR_ES6 -> dependencies + friends
            TargetBackend.JS -> JsConfig.JS_STDLIB + JsConfig.JS_KOTLIN_TEST + dependencies + friends
            else -> error("Unsupported target backend: ${module.targetBackend}")
        }
        configuration.put(JSConfigurationKeys.LIBRARIES, libraries)
        configuration.put(JSConfigurationKeys.TRANSITIVE_LIBRARIES, allDependencies)
        configuration.put(JSConfigurationKeys.FRIEND_PATHS, friends)

        configuration.put(CommonConfigurationKeys.MODULE_NAME, module.name.removeSuffix(OLD_MODULE_SUFFIX))
        configuration.put(JSConfigurationKeys.TARGET, EcmaVersion.es5)

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

        configuration.put(JSConfigurationKeys.GENERATE_POLYFILLS, true)
        configuration.put(JSConfigurationKeys.GENERATE_REGION_COMMENTS, true)

        configuration.put(
            JSConfigurationKeys.FILE_PATHS_PREFIX_MAP,
            mapOf(File(".").absolutePath.removeSuffix(".") to "")
        )
    }
}

