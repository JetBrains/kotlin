/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.js

import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.cli.js.klib.TopDownAnalyzerFacadeForJSIR
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.MutableModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.prepareAnalyzedSourceModule
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.K2JSTranslator
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationResult
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.serialization.AbstractVersionRequirementTest
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

class JsVersionRequirementTest : AbstractVersionRequirementTest() {
    override fun compileFiles(
        files: List<File>,
        outputDirectory: File,
        languageVersion: LanguageVersion,
        analysisFlags: Map<AnalysisFlag<*>, Any?>,
        specificFeatures: Map<LanguageFeature, LanguageFeature.State>
    ) {
        val environment = createEnvironment(languageVersion)
        val ktFiles = files.map { file ->
            KtTestUtil.createFile(
                file.name,
                file.readText(),
                environment.project
            )
        }

        val sourceModule = prepareAnalyzedSourceModule(
            environment.project,
            ktFiles,
            environment.configuration,
            environment.configuration[JSConfigurationKeys.LIBRARIES]!!.toList(),
            emptyList(),
            AnalyzerWithCompilerReport(environment.configuration),
            analyzerFacade = TopDownAnalyzerFacadeForJSIR
        )

        val analysisResult = sourceModule.jsFrontEndResult.jsAnalysisResult as JsAnalysisResult

        val result = K2JSTranslator(JsConfig(environment.project, environment.configuration, CompilerEnvironment)).translate(
            object : JsConfig.Reporter() {}, ktFiles, MainCallParameters.noCall(), analysisResult
        ) as TranslationResult.Success
        result.getOutputFiles(File(outputDirectory, "lib.js"), null, null).writeAllTo(outputDirectory)
    }

    override fun loadModule(directory: File): ModuleDescriptor {
        val environment = createEnvironment(extraDependencies = listOf(File(directory, "lib.meta.js")))
        return TopDownAnalyzerFacadeForJS.analyzeFilesWithGivenTrace(
            emptyList(), BindingTraceContext(), createModule(environment), environment.configuration, CompilerEnvironment, environment.project
        ).moduleDescriptor
    }

    private fun createEnvironment(
        languageVersion: LanguageVersion? = null,
        extraDependencies: List<File> = emptyList()
    ): KotlinCoreEnvironment =
        KotlinCoreEnvironment.createForTests(
            testRootDisposable,
            KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK).apply {
                put(JSConfigurationKeys.LIBRARIES, extraDependencies.map(File::getPath) + JsConfig.JS_STDLIB)
                put(JSConfigurationKeys.META_INFO, true)

                if (languageVersion != null) {
                    languageVersionSettings =
                            LanguageVersionSettingsImpl(languageVersion, ApiVersion.createByLanguageVersion(languageVersion))
                }
            },
            EnvironmentConfigFiles.JS_CONFIG_FILES
        )

    private fun createModule(environment: KotlinCoreEnvironment): MutableModuleContext {
        val config = JsConfig(environment.project, environment.configuration, CompilerEnvironment)
        return ContextForNewModule(
            ProjectContext(environment.project, "ProjectContext"),
            Name.special("<test>"), JsPlatformAnalyzerServices.builtIns, JsPlatforms.defaultJsPlatform
        ).apply {
            setDependencies(listOf(module) + config.moduleDescriptors + module.builtIns.builtInsModule)
        }
    }
}
