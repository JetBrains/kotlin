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
    @NotNull
    public static AnalysisResult analyzeAndCheckForErrors(@NotNull KtFile file, @NotNull KotlinCoreEnvironment environment) {
        return analyzeAndCheckForErrors(Collections.singleton(file), environment);
    }

    @NotNull
    public static AnalysisResult analyzeAndCheckForErrors(
            @NotNull Collection<KtFile> files,
            @NotNull KotlinCoreEnvironment environment
    ) {
        return analyzeAndCheckForErrors(
                environment.getProject(), files, environment.getConfiguration(), new JvmPackagePartProvider(environment)
        );
    }

    @NotNull
    public static AnalysisResult analyzeAndCheckForErrors(
            @NotNull Project project,
            @NotNull Collection<KtFile> files,
            @NotNull CompilerConfiguration configuration,
            @NotNull PackagePartProvider packagePartProvider
    ) {
        for (KtFile file : files) {
            AnalyzingUtils.checkForSyntacticErrors(file);
        }

        AnalysisResult analysisResult = analyze(project, files, configuration, packagePartProvider);

        AnalyzingUtils.throwExceptionOnErrors(analysisResult.getBindingContext());

        return analysisResult;
    }

    @NotNull
    public static AnalysisResult analyze(@NotNull KotlinCoreEnvironment environment) {
        return analyze(Collections.<KtFile>emptySet(), environment);
    }

    @NotNull
    public static AnalysisResult analyze(@NotNull KtFile file, @NotNull KotlinCoreEnvironment environment) {
        return analyze(Collections.singleton(file), environment);
    }

    @NotNull
    public static AnalysisResult analyze(@NotNull Collection<KtFile> files, @NotNull KotlinCoreEnvironment environment) {
        return analyze(environment.getProject(), files, environment.getConfiguration(), new JvmPackagePartProvider(environment));
    }

    @NotNull
    private static AnalysisResult analyze(
            @NotNull Project project,
            @NotNull Collection<KtFile> files,
            @NotNull CompilerConfiguration configuration,
            @NotNull PackagePartProvider packagePartProvider
    ) {
        return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(project, configuration),
                files, new CliLightClassGenerationSupport.CliBindingTrace(), configuration, packagePartProvider
        );
    }
}
