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

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.PackagePartProvider;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;

import java.util.Collections;
import java.util.List;

public class GenerationUtils {

    private GenerationUtils() {
    }

    @NotNull
    public static ClassFileFactory compileFileGetClassFileFactoryForTest(
            @NotNull JetFile psiFile,
            @NotNull KotlinCoreEnvironment environment
    ) {
        return compileFileGetGenerationStateForTest(psiFile, environment).getFactory();
    }

    @NotNull
    public static GenerationState compileFileGetGenerationStateForTest(
            @NotNull JetFile psiFile,
            @NotNull KotlinCoreEnvironment environment
    ) {
        AnalysisResult analysisResult =
                JvmResolveUtil.analyzeOneFileWithJavaIntegrationAndCheckForErrors(psiFile, new JvmPackagePartProvider(environment));
        return compileFilesGetGenerationState(psiFile.getProject(), analysisResult, Collections.singletonList(psiFile));
    }

    @NotNull
    public static GenerationState compileManyFilesGetGenerationStateForTest(@NotNull Project project, @NotNull List<JetFile> files) {
        return compileManyFilesGetGenerationStateForTest(project, files, PackagePartProvider.Companion.getEMPTY());
    }

    @NotNull
    public static GenerationState compileManyFilesGetGenerationStateForTest(@NotNull Project project, @NotNull List<JetFile> files,
            @NotNull PackagePartProvider packagePartProvider
    ) {
        AnalysisResult analysisResult = JvmResolveUtil.analyzeFilesWithJavaIntegrationAndCheckForErrors(
                project, files, packagePartProvider);
        return compileFilesGetGenerationState(project, analysisResult, files);
    }

    @NotNull
    public static GenerationState compileFilesGetGenerationState(
            @NotNull Project project,
            @NotNull AnalysisResult analysisResult,
            @NotNull List<JetFile> files
    ) {
        analysisResult.throwIfError();
        GenerationState state = new GenerationState(
                project, ClassBuilderFactories.TEST,
                analysisResult.getModuleDescriptor(), analysisResult.getBindingContext(),
                files,
                /* disableCallAssertions = */ false,
                /* disableParamAssertions = */ false
        );
        KotlinCodegenFacade.compileCorrectFiles(state, CompilationErrorHandler.THROW_EXCEPTION);
        return state;
    }
}
