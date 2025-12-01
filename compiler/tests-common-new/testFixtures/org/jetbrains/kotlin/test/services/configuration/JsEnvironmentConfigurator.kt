/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_JS_KOTLIN_TEST_KLIB_PATH
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_JS_STDLIB_KLIB_PATH
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.js.config.moduleKind
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.JS_MODULE_KIND
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.util.joinToArrayString
import java.io.File

abstract class JsEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(JsEnvironmentConfigurationDirectives, KlibBasedCompilerTestDirectives)

    companion object : KlibBasedEnvironmentConfiguratorUtils {
        const val TEST_DATA_DIR_PATH = "js/js.translator/testData"
        const val OLD_MODULE_SUFFIX = "_old"

        val kotlinTestPath: String
            get() = System.getProperty(KOTLIN_JS_KOTLIN_TEST_KLIB_PATH)!!

        val stdlibPath: String
            get() = System.getProperty(KOTLIN_JS_STDLIB_KLIB_PATH)!!

        // Keep names short to keep path lengths under 255 for Windows
        private val outputDirByMode = mapOf(
            TranslationMode.FULL_DEV to "out",
            TranslationMode.FULL_PROD_MINIMIZED_NAMES to "outMin",
            TranslationMode.PER_MODULE_DEV to "outPm",
            TranslationMode.PER_MODULE_PROD_MINIMIZED_NAMES to "outPmMin",
            TranslationMode.PER_FILE_DEV to "outPf",
            TranslationMode.PER_FILE_PROD_MINIMIZED_NAMES to "outPfMin"
        )

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

        fun isFullJsRuntimeNeeded(module: TestModule): Boolean =
            JsEnvironmentConfigurationDirectives.KJS_WITH_FULL_RUNTIME in module.directives || ConfigurationDirectives.WITH_STDLIB in module.directives

        fun getRuntimePathsForModule(module: TestModule, testServices: TestServices): List<String> {
            val result = mutableListOf<String>()

            val pathProvider = testServices.standardLibrariesPathProvider
            if (isFullJsRuntimeNeeded(module)) {
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

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.phaseConfig = createJsTestPhaseConfig(testServices, module)

        val registeredDirectives = module.directives
        val moduleKinds = registeredDirectives[JS_MODULE_KIND]
        val moduleKind = when (moduleKinds.size) {
            0 -> testServices.moduleStructure.allDirectives[JS_MODULE_KIND].singleOrNull()
                ?: if (JsEnvironmentConfigurationDirectives.ES_MODULES in registeredDirectives) ModuleKind.ES else ModuleKind.PLAIN
            1 -> moduleKinds.single()
            else -> error("Too many module kinds passed ${moduleKinds.joinToArrayString()}")
        }
        configuration.moduleKind = moduleKind
        configuration.moduleName = module.name.removeSuffix(OLD_MODULE_SUFFIX)
    }
}

