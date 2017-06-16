/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.lazy

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory

object JvmResolveUtil {
    @JvmStatic
    @JvmOverloads
    fun createContainer(environment: KotlinCoreEnvironment, files: Collection<KtFile> = emptyList()): ComponentProvider =
            TopDownAnalyzerFacadeForJVM.createContainer(
                    environment.project, files, CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace(),
                    environment.configuration, { PackagePartProvider.Empty }, ::FileBasedDeclarationProviderFactory
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
            packagePartProvider: (GlobalSearchScope) -> PackagePartProvider
    ): AnalysisResult {
        for (file in files) {
            AnalyzingUtils.checkForSyntacticErrors(file)
        }

        return analyze(project, files, configuration, packagePartProvider).apply {
            AnalyzingUtils.throwExceptionOnErrors(bindingContext)
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

    private fun analyze(
            project: Project,
            files: Collection<KtFile>,
            configuration: CompilerConfiguration,
            packagePartProviderFactory: (GlobalSearchScope) -> PackagePartProvider
    ): AnalysisResult {
        return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                project, files, CliLightClassGenerationSupport.CliBindingTrace(), configuration, packagePartProviderFactory
        )
    }
}
