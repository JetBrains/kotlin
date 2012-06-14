/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.analyzer.AnalyzerFacade;
import org.jetbrains.jet.analyzer.AnalyzerFacadeForEverything;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.ObservableBindingTrace;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.CompilerDependencies.compilerDependenciesForProduction;

/**
 * @author abreslav
 */
public enum AnalyzerFacadeForJVM implements AnalyzerFacade {

    INSTANCE;

    private AnalyzerFacadeForJVM() {
    }

    @Override
    @NotNull
    public AnalyzeExhaust analyzeFiles(@NotNull Project project,
            @NotNull Collection<JetFile> files,
            @NotNull List<AnalyzerScriptParameter> scriptParameters,
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely) {
        return analyzeFilesWithJavaIntegration(project, files, scriptParameters, filesToAnalyzeCompletely,
                                               compilerDependenciesForProduction(CompilerSpecialMode.REGULAR), true);
    }

    @NotNull
    @Override
    public AnalyzeExhaust analyzeBodiesInFiles(@NotNull Project project,
                                               @NotNull List<AnalyzerScriptParameter> scriptParameters,
                                               @NotNull Predicate<PsiFile> filesForBodiesResolve,
                                               @NotNull BindingTrace headersTraceContext,
                                               @NotNull BodiesResolveContext bodiesResolveContext
    ) {
        return AnalyzerFacadeForEverything.analyzeBodiesInFilesWithJavaIntegration(
                project, scriptParameters, filesForBodiesResolve,
                headersTraceContext, bodiesResolveContext);
    }

    public static AnalyzeExhaust analyzeOneFileWithJavaIntegrationAndCheckForErrors(
            JetFile file, List<AnalyzerScriptParameter> scriptParameters, @NotNull CompilerDependencies compilerDependencies) {
        AnalyzingUtils.checkForSyntacticErrors(file);

        AnalyzeExhaust analyzeExhaust = analyzeOneFileWithJavaIntegration(file, scriptParameters, compilerDependencies);

        AnalyzingUtils.throwExceptionOnErrors(analyzeExhaust.getBindingContext());

        return analyzeExhaust;
    }

    public static AnalyzeExhaust analyzeOneFileWithJavaIntegration(
            JetFile file, List<AnalyzerScriptParameter> scriptParameters, @NotNull CompilerDependencies compilerDependencies) {
        return analyzeFilesWithJavaIntegration(file.getProject(), Collections.singleton(file), scriptParameters,
                                               Predicates.<PsiFile>alwaysTrue(), compilerDependencies);
    }

    public static AnalyzeExhaust analyzeFilesWithJavaIntegrationAndCheckForErrors(
            Project project, Collection<JetFile> files, List<AnalyzerScriptParameter> scriptParameters, Predicate<PsiFile> filesToAnalyzeCompletely,
            @NotNull CompilerDependencies compilerDependencies) {
        for (JetFile file : files) {
            AnalyzingUtils.checkForSyntacticErrors(file); 
        }
       
        AnalyzeExhaust analyzeExhaust = analyzeFilesWithJavaIntegration(
                project, files, scriptParameters, filesToAnalyzeCompletely, compilerDependencies, false);

        AnalyzingUtils.throwExceptionOnErrors(analyzeExhaust.getBindingContext());

        return analyzeExhaust;
    }

    public static AnalyzeExhaust analyzeFilesWithJavaIntegration(
            Project project, Collection<JetFile> files, List<AnalyzerScriptParameter> scriptParameters, Predicate<PsiFile> filesToAnalyzeCompletely,
            @NotNull CompilerDependencies compilerDependencies) {
        return analyzeFilesWithJavaIntegration(
                project, files, scriptParameters, filesToAnalyzeCompletely, compilerDependencies, false);
    }

    public static AnalyzeExhaust analyzeFilesWithJavaIntegration(
            Project project, Collection<JetFile> files, List<AnalyzerScriptParameter> scriptParameters, Predicate<PsiFile> filesToAnalyzeCompletely,
            @NotNull CompilerDependencies compilerDependencies,
            boolean storeContextForBodiesResolve) {
        BindingTraceContext bindingTraceContext = new BindingTraceContext();

        final ModuleDescriptor owner = new ModuleDescriptor(Name.special("<module>"));

        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(
                filesToAnalyzeCompletely, false, false, scriptParameters);

        InjectorForTopDownAnalyzerForJvm injector = new InjectorForTopDownAnalyzerForJvm(
                project, topDownAnalysisParameters,
                new ObservableBindingTrace(bindingTraceContext), owner, compilerDependencies);
        try {
            injector.getTopDownAnalyzer().analyzeFiles(files, scriptParameters);
            BodiesResolveContext bodiesResolveContext = storeContextForBodiesResolve ?
                                                        new CachedBodiesResolveContext(injector.getTopDownAnalysisContext()) :
                                                        null;
            return AnalyzeExhaust.success(bindingTraceContext.getBindingContext(), JetStandardLibrary.getInstance(), bodiesResolveContext);
        } finally {
            injector.destroy();
        }
    }

    public static AnalyzeExhaust shallowAnalyzeFiles(Collection<JetFile> files,
            @NotNull CompilerSpecialMode compilerSpecialMode, @NotNull CompilerDependencies compilerDependencies) {
        assert files.size() > 0;

        Project project = files.iterator().next().getProject();

        return analyzeFilesWithJavaIntegration(project,
                files, Collections.<AnalyzerScriptParameter>emptyList(), Predicates.<PsiFile>alwaysFalse(),
                compilerDependencies);
    }
}
