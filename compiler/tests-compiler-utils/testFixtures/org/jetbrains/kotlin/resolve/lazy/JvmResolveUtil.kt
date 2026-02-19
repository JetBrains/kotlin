/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.lazy

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.K1_DEPRECATION_WARNING
import org.jetbrains.kotlin.TestsCompiletimeError
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.skipBodies
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
    @Deprecated(K1_DEPRECATION_WARNING, level = DeprecationLevel.ERROR)
    fun createContainer(
        environment: KotlinCoreEnvironment,
        files: Collection<KtFile> = emptyList(),
        targetEnvironment: TargetEnvironment = CompilerEnvironment
    ): ComponentProvider =
        @Suppress("DEPRECATION_ERROR")
        TopDownAnalyzerFacadeForJVM.createContainer(
            environment.project, files, NoScopeRecordCliBindingTrace(environment.project),
            environment.configuration, { PackagePartProvider.Empty }, ::FileBasedDeclarationProviderFactory,
            targetEnvironment
        )

    @JvmStatic
    @Deprecated(K1_DEPRECATION_WARNING, level = DeprecationLevel.ERROR)
    fun analyzeAndCheckForErrors(file: KtFile, environment: KotlinCoreEnvironment): AnalysisResult {
        @Suppress("DEPRECATION_ERROR")
        return analyzeAndCheckForErrors(environment.project, setOf(file), environment.configuration, environment::createPackagePartProvider)
    }

    @JvmStatic
    @Deprecated(K1_DEPRECATION_WARNING, level = DeprecationLevel.ERROR)
    fun analyzeAndCheckForErrors(
        project: Project,
        files: Collection<KtFile>,
        configuration: CompilerConfiguration,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        trace: BindingTrace = CliBindingTrace(project),
        klibList: List<KotlinLibrary> = emptyList()
    ): AnalysisResult {
        for (file in files) {
            try {
                AnalyzingUtils.checkForSyntacticErrors(file)
            } catch (e: Exception) {
                throw TestsCompiletimeError(e)
            }
        }

        @Suppress("DEPRECATION_ERROR")
        return analyze(project, files, configuration, packagePartProvider, trace, klibList).apply {
            try {
                // Do not report UNRESOLVED_REFERENCE in KAPT mode
                if (!configuration.skipBodies) {
                    AnalyzingUtils.throwExceptionOnErrors(bindingContext)
                }
            } catch (e: Exception) {
                throw TestsCompiletimeError(e)
            }
        }
    }

    @JvmStatic
    @Deprecated(K1_DEPRECATION_WARNING, level = DeprecationLevel.ERROR)
    fun analyze(environment: KotlinCoreEnvironment): AnalysisResult {
        @Suppress("DEPRECATION_ERROR")
        return analyze(emptySet(), environment)
    }

    @JvmStatic
    @Deprecated(K1_DEPRECATION_WARNING, level = DeprecationLevel.ERROR)
    fun analyze(file: KtFile, environment: KotlinCoreEnvironment): AnalysisResult {
        @Suppress("DEPRECATION_ERROR")
        return analyze(setOf(file), environment)
    }

    @JvmStatic
    @Deprecated(K1_DEPRECATION_WARNING, level = DeprecationLevel.ERROR)
    fun analyze(files: Collection<KtFile>, environment: KotlinCoreEnvironment): AnalysisResult {
        @Suppress("DEPRECATION_ERROR")
        return analyze(files, environment, environment.configuration)
    }

    @JvmStatic
    @Deprecated(K1_DEPRECATION_WARNING, level = DeprecationLevel.ERROR)
    fun analyze(files: Collection<KtFile>, environment: KotlinCoreEnvironment, configuration: CompilerConfiguration): AnalysisResult {
        @Suppress("DEPRECATION_ERROR")
        return analyze(environment.project, files, configuration, environment::createPackagePartProvider)
    }

    @Deprecated(K1_DEPRECATION_WARNING, level = DeprecationLevel.ERROR)
    fun analyze(
        project: Project,
        files: Collection<KtFile>,
        configuration: CompilerConfiguration,
        packagePartProviderFactory: (GlobalSearchScope) -> PackagePartProvider,
        trace: BindingTrace = CliBindingTrace(project),
        klibList: List<KotlinLibrary> = emptyList()
    ): AnalysisResult {
        @Suppress("DEPRECATION_ERROR")
        return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
            project, files, trace, configuration, packagePartProviderFactory,
            klibList = klibList
        )
    }
}
