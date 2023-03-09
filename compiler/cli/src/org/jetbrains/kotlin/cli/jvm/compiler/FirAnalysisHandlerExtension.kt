/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

abstract class FirAnalysisHandlerExtension {
    companion object : ProjectExtensionDescriptor<FirAnalysisHandlerExtension>(
        "org.jetbrains.kotlin.fir.firAnalyzeCompleteHandlerExtension",
        FirAnalysisHandlerExtension::class.java
    )

    abstract fun doAnalysis(
        ktAnalysisSession: KtAnalysisSession,
        lightClasses: Map<KtLightClass, KtFile>
    ): K2AnalysisResult
}

sealed class K2AnalysisResult(val success: Boolean) {
    object DoNotGenerateCode : K2AnalysisResult(success = true)
    class RetryWithAdditionalRoots(
        val additionalJavaRoots: List<File>,
        val additionalKotlinRoots: List<File>,
        val additionalClassPathRoots: List<File> = emptyList()
    ) : K2AnalysisResult(success = false)

    object CompilationError : K2AnalysisResult(success = false)
    class InternalError(val exception: Exception) : K2AnalysisResult(success = false)
}
