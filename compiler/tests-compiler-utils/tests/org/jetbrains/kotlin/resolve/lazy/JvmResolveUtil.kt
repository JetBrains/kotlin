/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.lazy

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.TestsCompiletimeError
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory

object JvmResolveUtil {
    @JvmStatic
    @JvmOverloads
    fun createContainer(
        environment: KotlinCoreEnvironment,
        files: Collection<KtFile> = emptyList(),
        targetEnvironment: TargetEnvironment = CompilerEnvironment
    ): ComponentProvider =
        TopDownAnalyzerFacadeForJVM.createContainer(
            environment.project, files, NoScopeRecordCliBindingTrace(),
            environment.configuration, { PackagePartProvider.Empty }, ::FileBasedDeclarationProviderFactory,
            targetEnvironment
        )

    @JvmStatic
    fun analyzeAndCheckForErrors(file: KtFile, environment: KotlinCoreEnvironment): AnalysisResult =
        analyzeAndCheckForErrors(setOf(file), environment)

    @JvmStatic
    fun analyzeAndCheckForErrors(files: Collection<KtFile>, environment: KotlinCoreEnvironment): AnalysisResult =
        analyzeAndCheckForErrors(environment.project, files, environment.configuration, environment::createPackagePartProvider)

    @JvmStatic
    fun analyzeAndCheckForErrors(
        project: Project,
        files: Collection<KtFile>,
        configuration: CompilerConfiguration,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        trace: BindingTrace = CliBindingTrace(),
        klibList: List<KotlinLibrary> = emptyList()
    ): AnalysisResult {
        for (file in files) {
            try {
                AnalyzingUtils.checkForSyntacticErrors(file)
            } catch (e: Exception) {
                throw TestsCompiletimeError(e)
            }
        }

        return analyze(project, files, configuration, packagePartProvider, trace, klibList).apply {
            try {
                AnalyzingUtils.throwExceptionOnErrors(bindingContext)
            } catch (e: Exception) {
                throw TestsCompiletimeError(e)
            }
        }
    }

    @JvmStatic
    fun analyze(environment: KotlinCoreEnvironment): AnalysisResult =
        analyze(emptySet(), environment)

    @JvmStatic
    fun analyze(file: KtFile, environment: KotlinCoreEnvironment): AnalysisResult =
        analyze(setOf(file), environment)

    @JvmStatic
    fun analyze(files: Collection<KtFile>, environment: KotlinCoreEnvironment): AnalysisResult =
        analyze(files, environment, environment.configuration)

    @JvmStatic
    fun analyze(files: Collection<KtFile>, environment: KotlinCoreEnvironment, configuration: CompilerConfiguration): AnalysisResult =
        analyze(environment.project, files, configuration, environment::createPackagePartProvider)

    fun analyze(
        project: Project,
        files: Collection<KtFile>,
        configuration: CompilerConfiguration,
        packagePartProviderFactory: (GlobalSearchScope) -> PackagePartProvider,
        trace: BindingTrace = CliBindingTrace(),
        klibList: List<KotlinLibrary> = emptyList()
    ): AnalysisResult {
        return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
            project, files, trace, configuration, packagePartProviderFactory,
            klibList = klibList
        )
    }
}
