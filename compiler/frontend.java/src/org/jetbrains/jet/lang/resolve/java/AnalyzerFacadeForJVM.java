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
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.util.Collection;
import java.util.Collections;

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
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely,
            @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory) {
        return analyzeFilesWithJavaIntegration(project, files, filesToAnalyzeCompletely, flowDataTraceFactory,
                                               compilerDependenciesForProduction(CompilerSpecialMode.REGULAR), true);
    }

    @NotNull
    @Override
    public AnalyzeExhaust analyzeBodiesInFiles(@NotNull Project project,
                                               @NotNull Predicate<PsiFile> filesForBodiesResolve,
                                               @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory,
                                               @NotNull BindingTrace headersTraceContext,
                                               @NotNull BodiesResolveContext bodiesResolveContext
    ) {
        return analyzeBodiesInFilesWithJavaIntegration(
                project, filesForBodiesResolve, flowDataTraceFactory,
                compilerDependenciesForProduction(CompilerSpecialMode.REGULAR),
                headersTraceContext, bodiesResolveContext);
    }

    public static AnalyzeExhaust analyzeOneFileWithJavaIntegrationAndCheckForErrors(
            JetFile file, JetControlFlowDataTraceFactory flowDataTraceFactory,
            @NotNull CompilerDependencies compilerDependencies) {
        AnalyzingUtils.checkForSyntacticErrors(file);

        AnalyzeExhaust analyzeExhaust = analyzeOneFileWithJavaIntegration(file, flowDataTraceFactory, compilerDependencies);

        AnalyzingUtils.throwExceptionOnErrors(analyzeExhaust.getBindingContext());

        return analyzeExhaust;
    }

    public static AnalyzeExhaust analyzeOneFileWithJavaIntegration(
            JetFile file, JetControlFlowDataTraceFactory flowDataTraceFactory,
            @NotNull CompilerDependencies compilerDependencies) {
        return analyzeFilesWithJavaIntegration(file.getProject(), Collections.singleton(file),
                                               Predicates.<PsiFile>alwaysTrue(), flowDataTraceFactory, compilerDependencies);
    }

    public static AnalyzeExhaust analyzeFilesWithJavaIntegration(
            Project project, Collection<JetFile> files, Predicate<PsiFile> filesToAnalyzeCompletely,
            JetControlFlowDataTraceFactory flowDataTraceFactory,
            @NotNull CompilerDependencies compilerDependencies) {
        return analyzeFilesWithJavaIntegration(
                project, files, filesToAnalyzeCompletely,
                flowDataTraceFactory, compilerDependencies, false);
    }

    public static AnalyzeExhaust analyzeFilesWithJavaIntegration(
            Project project, Collection<JetFile> files, Predicate<PsiFile> filesToAnalyzeCompletely,
            JetControlFlowDataTraceFactory flowDataTraceFactory,
            @NotNull CompilerDependencies compilerDependencies,
            boolean storeContextForBodiesResolve) {
        BindingTraceContext bindingTraceContext = new BindingTraceContext();

        final ModuleDescriptor owner = new ModuleDescriptor(Name.special("<module>"));

        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(
                filesToAnalyzeCompletely, false, false);

        InjectorForTopDownAnalyzerForJvm injector = new InjectorForTopDownAnalyzerForJvm(
                project, topDownAnalysisParameters,
                new ObservableBindingTrace(bindingTraceContext), owner, flowDataTraceFactory,
                compilerDependencies);
        try {
            injector.getTopDownAnalyzer().analyzeFiles(files);
            BodiesResolveContext bodiesResolveContext = storeContextForBodiesResolve ?
                                                        new CachedBodiesResolveContext(injector.getTopDownAnalysisContext()) :
                                                        null;
            return AnalyzeExhaust.success(bindingTraceContext.getBindingContext(), JetStandardLibrary.getInstance(), bodiesResolveContext);
        } finally {
            injector.destroy();
        }
    }

    public static AnalyzeExhaust analyzeBodiesInFilesWithJavaIntegration(
            Project project, Predicate<PsiFile> filesToAnalyzeCompletely,
            JetControlFlowDataTraceFactory flowDataTraceFactory,
            @NotNull CompilerDependencies compilerDependencies,
            @NotNull BindingTrace traceContext,
            @NotNull BodiesResolveContext bodiesResolveContext) {
        final ModuleDescriptor owner = new ModuleDescriptor(Name.special("<module>"));

        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(
                filesToAnalyzeCompletely, false, false);

        bodiesResolveContext.setTopDownAnalysisParameters(topDownAnalysisParameters);

        InjectorForTopDownAnalyzerForJvm injector = new InjectorForTopDownAnalyzerForJvm(
                project, topDownAnalysisParameters,
                new ObservableBindingTrace(traceContext), owner, flowDataTraceFactory,
                compilerDependencies);

        try {
            injector.getTopDownAnalyzer().doProcessForBodies(bodiesResolveContext);
            return AnalyzeExhaust.success(traceContext.getBindingContext(), JetStandardLibrary.getInstance());
        } finally {
            injector.destroy();
        }
    }

    public static AnalyzeExhaust shallowAnalyzeFiles(Collection<JetFile> files,
            @NotNull CompilerSpecialMode compilerSpecialMode, @NotNull CompilerDependencies compilerDependencies) {
        assert files.size() > 0;

        Project project = files.iterator().next().getProject();

        return analyzeFilesWithJavaIntegration(project, files, Predicates.<PsiFile>alwaysFalse(),
                                               JetControlFlowDataTraceFactory.EMPTY, compilerDependencies);
    }
}
