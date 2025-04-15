/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.js.klib.TopDownAnalyzerFacadeForJSIR
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.analyze.AbstractTopDownAnalyzerFacadeForWeb
import org.jetbrains.kotlin.psi.KtFile

fun prepareAnalyzedSourceModule(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    klibs: LoadedKlibs,
    analyzer: AbstractAnalyzerWithCompilerReport,
    analyzerFacade: AbstractTopDownAnalyzerFacadeForWeb = TopDownAnalyzerFacadeForJSIR,
): ModulesStructure {
    val mainModule = MainModule.SourceFiles(files)
    val sourceModule = ModulesStructure(
        project = project,
        mainModule = mainModule,
        compilerConfiguration = configuration,
        klibs = klibs,
    )
    return sourceModule.apply {
        runAnalysis(analyzer, analyzerFacade)
    }
}