/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analyzer

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.TargetEnvironment

interface AbstractAnalyzerWithCompilerReport {
    val targetEnvironment: TargetEnvironment

    val analysisResult: AnalysisResult

    fun analyzeAndReport(files: Collection<KtFile>, analyze: () -> AnalysisResult)

    fun hasErrors(): Boolean
}
