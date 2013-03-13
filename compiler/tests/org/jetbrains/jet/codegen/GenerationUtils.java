/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;

import java.util.Collections;
import java.util.List;

public class GenerationUtils {

    private GenerationUtils() {
    }

    @NotNull
    public static ClassFileFactory compileFileGetClassFileFactoryForTest(@NotNull JetFile psiFile) {
        return compileFileGetGenerationStateForTest(psiFile).getFactory();
    }

    @NotNull
    public static GenerationState compileFileGetGenerationStateForTest(@NotNull JetFile psiFile) {
        AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegrationAndCheckForErrors(
                psiFile, Collections.<AnalyzerScriptParameter>emptyList());
        return compileFilesGetGenerationState(psiFile.getProject(), analyzeExhaust, Collections.singletonList(psiFile));
    }

    @NotNull
    public static GenerationState compileManyFilesGetGenerationStateForTest(@NotNull Project project, @NotNull List<JetFile> files) {
        AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegrationAndCheckForErrors(
                project, files, Collections.<AnalyzerScriptParameter>emptyList(), Predicates.<PsiFile>alwaysTrue());
        return compileFilesGetGenerationState(project,analyzeExhaust, files);
    }


    @NotNull
    public static GenerationState compileFilesGetGenerationState(@NotNull Project project, @NotNull AnalyzeExhaust analyzeExhaust, @NotNull List<JetFile> files) {
        analyzeExhaust.throwIfError();
        GenerationState state = new GenerationState(project, ClassBuilderFactories.TEST, analyzeExhaust.getBindingContext(), files);
        KotlinCodegenFacade.compileCorrectFiles(state, CompilationErrorHandler.THROW_EXCEPTION);
        return state;
    }
}
