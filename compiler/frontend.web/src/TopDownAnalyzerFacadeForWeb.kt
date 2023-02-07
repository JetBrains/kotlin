/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.web.analyze

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.js.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.js.analyzer.WebAnalysisResult

interface AbstractTopDownAnalyzerFacadeForWeb {
    fun analyzeFiles(
        files: Collection<KtFile>,
        project: Project,
        configuration: CompilerConfiguration,
        moduleDescriptors: List<ModuleDescriptor>,
        friendModuleDescriptors: List<ModuleDescriptor>,
        targetEnvironment: TargetEnvironment,
        thisIsBuiltInsModule: Boolean = false,
        customBuiltInsModule: ModuleDescriptor? = null
    ): WebAnalysisResult

    fun analyzeFilesWithGivenTrace(
        files: Collection<KtFile>,
        trace: BindingTrace,
        moduleContext: ModuleContext,
        configuration: CompilerConfiguration,
        targetEnvironment: TargetEnvironment,
        project: Project,
        additionalPackages: List<PackageFragmentProvider> = emptyList()
    ): WebAnalysisResult

    fun checkForErrors(allFiles: Collection<KtFile>, bindingContext: BindingContext, errorPolicy: ErrorTolerancePolicy): Boolean {
        var hasErrors = false
        try {
            AnalyzingUtils.throwExceptionOnErrors(bindingContext)
        } catch (ex: Exception) {
            if (!errorPolicy.allowSemanticErrors) {
                throw ex
            } else {
                hasErrors = true
            }
        }

        try {
            for (file in allFiles) {
                AnalyzingUtils.checkForSyntacticErrors(file)
            }
        } catch (ex: Exception) {
            if (!errorPolicy.allowSyntaxErrors) {
                throw ex
            } else {
                hasErrors = true
            }
        }

        return hasErrors
    }
}