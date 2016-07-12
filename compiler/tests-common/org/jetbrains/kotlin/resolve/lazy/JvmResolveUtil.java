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

package org.jetbrains.kotlin.resolve.lazy;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.descriptors.PackagePartProvider;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.AnalyzingUtils;
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM;

import java.util.Collection;
import java.util.Collections;

public class JvmResolveUtil {
    public static String TEST_MODULE_NAME = "java-integration-test";

    @NotNull
    public static AnalysisResult analyzeOneFileWithJavaIntegrationAndCheckForErrors(@NotNull KtFile file) {
        return analyzeOneFileWithJavaIntegrationAndCheckForErrors(file, PackagePartProvider.Companion.getEMPTY());
    }

    @NotNull
    public static AnalysisResult analyzeOneFileWithJavaIntegrationAndCheckForErrors(@NotNull KtFile file, @NotNull PackagePartProvider provider) {
        AnalyzingUtils.checkForSyntacticErrors(file);

        AnalysisResult analysisResult = analyzeOneFileWithJavaIntegration(file, provider);

        AnalyzingUtils.throwExceptionOnErrors(analysisResult.getBindingContext());

        return analysisResult;
    }

    @NotNull
    public static AnalysisResult analyzeOneFileWithJavaIntegration(@NotNull KtFile file, @NotNull KotlinCoreEnvironment environment) {
        return analyzeOneFileWithJavaIntegration(file, new JvmPackagePartProvider(environment));
    }

    @NotNull
    private static AnalysisResult analyzeOneFileWithJavaIntegration(@NotNull KtFile file, @NotNull PackagePartProvider provider) {
        return analyzeFilesWithJavaIntegration(file.getProject(), Collections.singleton(file), provider);
    }

    @NotNull
    public static AnalysisResult analyzeOneFileWithJavaIntegration(@NotNull KtFile file) {
        return analyzeOneFileWithJavaIntegration(file, PackagePartProvider.Companion.getEMPTY());
    }

    @NotNull
    public static AnalysisResult analyzeFilesWithJavaIntegrationAndCheckForErrors(
            @NotNull Project project,
            @NotNull Collection<KtFile> files
    ) {
        return analyzeFilesWithJavaIntegrationAndCheckForErrors(project, files, PackagePartProvider.Companion.getEMPTY());
    }

    @NotNull
    public static AnalysisResult analyzeFilesWithJavaIntegrationAndCheckForErrors(
            @NotNull Project project,
            @NotNull Collection<KtFile> files,
            @NotNull PackagePartProvider packagePartProvider
    ) {
        for (KtFile file : files) {
            AnalyzingUtils.checkForSyntacticErrors(file);
        }

        AnalysisResult analysisResult = analyzeFilesWithJavaIntegration(project, files, packagePartProvider);

        AnalyzingUtils.throwExceptionOnErrors(analysisResult.getBindingContext());

        return analysisResult;
    }

    @NotNull
    public static AnalysisResult analyzeFilesWithJavaIntegration(
            @NotNull Project project,
            @NotNull Collection<KtFile> files,
            @NotNull KotlinCoreEnvironment environment
    ) {
        return analyzeFilesWithJavaIntegration(project, files, new JvmPackagePartProvider(environment));
    }

    @NotNull
    private static AnalysisResult analyzeFilesWithJavaIntegration(
            @NotNull Project project,
            @NotNull Collection<KtFile> files,
            @NotNull PackagePartProvider packagePartProvider
    ) {
        return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegrationWithCustomContext(
                TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(project, TEST_MODULE_NAME),
                files, new CliLightClassGenerationSupport.CliBindingTrace(), CompilerConfiguration.EMPTY, packagePartProvider
        );
    }
}
