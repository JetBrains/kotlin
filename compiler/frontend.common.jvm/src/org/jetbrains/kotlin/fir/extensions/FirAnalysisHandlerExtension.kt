/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor


abstract class FirAnalysisHandlerExtension {
    companion object : ProjectExtensionDescriptor<FirAnalysisHandlerExtension>(
        "org.jetbrains.kotlin.fir.firAnalyzeCompleteHandlerExtension",
        FirAnalysisHandlerExtension::class.java
    ) {
        /**
         * Applies [FirAnalysisHandlerExtension] instances to a project
         * @receiver the project to analyze
         * @param configuration compiler configuration
         * @return [null] if no applicable extensions were found, [true] if all applicable extensions returned [true] from [doAnalysis],
         * [false] if any applicable extension returned [false]
         *
         * @see FirAnalysisHandlerExtension.isApplicable
         * @see FirAnalysisHandlerExtension.doAnalysis
         */
        fun analyze(project: Project, configuration: CompilerConfiguration): Boolean? {
            val extensions = FirAnalysisHandlerExtension.getInstances(project).filter { it.isApplicable(configuration) }
            return if (extensions.isEmpty()) null else extensions.all { it.doAnalysis(configuration) }
        }
    }

    /**
     * Checks whether [doAnalysis] should be called
     * @param configuration compiler configuration
     * @return true if [doAnalysis] should be called
     */
    abstract fun isApplicable(configuration: CompilerConfiguration): Boolean

    /**
     * Performs code analysis
     * @param configuration compiler configuration
     * @return [true] if analysis completed successfully, [false] otherwise.
     * There can be different causes of failure, an incorrect configuration for example.
     * A failure means that there's no reason to continue building the project.
     */
    abstract fun doAnalysis(configuration: CompilerConfiguration): Boolean
}