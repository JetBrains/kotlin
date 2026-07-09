/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.lazy

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.K1_DEPRECATION_WARNING
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace

object JvmResolveUtil {
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
    ): AnalysisResult {
        @Suppress("DEPRECATION_ERROR")
        return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(project, files, trace, configuration, packagePartProviderFactory)
    }
}
