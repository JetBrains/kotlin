/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace

abstract class AbstractDiagnosticsTestWithJvmBackend : AbstractDiagnosticsTest() {

    override fun analyzeModuleContents(
        moduleContext: ModuleContext,
        files: List<KtFile>,
        moduleTrace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
        separateModules: Boolean,
        jvmTarget: JvmTarget
    ): AnalysisResult {
        val analysisResult =
            super.analyzeModuleContents(
                moduleContext, files, moduleTrace, languageVersionSettings, separateModules, jvmTarget
            )

        val generationState =
            GenerationUtils.generateFiles(project, files, environment.configuration, ClassBuilderFactories.TEST, analysisResult)

        for (diagnostic in generationState.collectedExtraJvmDiagnostics.all()) {
            moduleTrace.report(diagnostic)
        }
        return analysisResult
    }

    // DO NOT use FE-based diagnostics for JVM signature conflict
    override fun shouldSkipJvmSignatureDiagnostics(groupedByModule: Map<TestModule?, List<TestFile>>): Boolean = true
}

abstract class AbstractDiagnosticsTestWithOldJvmBackend : AbstractDiagnosticsTestWithJvmBackend()

abstract class AbstractDiagnosticsTestWithJvmIrBackend : AbstractDiagnosticsTestWithJvmBackend() {
    override fun updateConfiguration(configuration: CompilerConfiguration) {
        configuration.put(JVMConfigurationKeys.IR, true)
    }
}
