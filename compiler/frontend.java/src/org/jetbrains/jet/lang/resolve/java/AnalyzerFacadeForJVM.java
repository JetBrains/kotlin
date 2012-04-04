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
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.analyzer.AnalyzerFacade;
import org.jetbrains.jet.analyzer.AnalyzerFacadeWithCache;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.ObservableBindingTrace;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.util.Collection;
import java.util.Collections;

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
        return analyzeFilesWithJavaIntegration(project, files, filesToAnalyzeCompletely, flowDataTraceFactory, CompilerSpecialMode.REGULAR);
    }

    public static AnalyzeExhaust analyzeOneFileWithJavaIntegrationAndCheckForErrors(
        JetFile file, JetControlFlowDataTraceFactory flowDataTraceFactory) {
        AnalyzingUtils.checkForSyntacticErrors(file);

        AnalyzeExhaust analyzeExhaust = analyzeOneFileWithJavaIntegration(file, flowDataTraceFactory);

        AnalyzingUtils.throwExceptionOnErrors(analyzeExhaust.getBindingContext());

        return analyzeExhaust;
    }

    public static AnalyzeExhaust analyzeOneFileWithJavaIntegration(JetFile file, JetControlFlowDataTraceFactory flowDataTraceFactory) {
        return analyzeFilesWithJavaIntegration(file.getProject(), Collections.singleton(file),
                                               Predicates.<PsiFile>alwaysTrue(), flowDataTraceFactory, CompilerSpecialMode.REGULAR);
    }

    public static AnalyzeExhaust analyzeFilesWithJavaIntegration(
        Project project, Collection<JetFile> files, Predicate<PsiFile> filesToAnalyzeCompletely,
        JetControlFlowDataTraceFactory flowDataTraceFactory,
        CompilerSpecialMode compilerSpecialMode) {
        BindingTraceContext bindingTraceContext = new BindingTraceContext();

        final ModuleDescriptor owner = new ModuleDescriptor("<module>");

        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(
            filesToAnalyzeCompletely, false, false);


        InjectorForTopDownAnalyzerForJvm injector = new InjectorForTopDownAnalyzerForJvm(
            project, topDownAnalysisParameters,
            new ObservableBindingTrace(bindingTraceContext), owner, flowDataTraceFactory,
            compilerSpecialMode);


        injector.getTopDownAnalyzer().analyzeFiles(files);
        return new AnalyzeExhaust(bindingTraceContext.getBindingContext(), JetStandardLibrary.getInstance());
    }

    public static AnalyzeExhaust shallowAnalyzeFiles(Collection<JetFile> files) {
        assert files.size() > 0;

        Project project = files.iterator().next().getProject();

        return analyzeFilesWithJavaIntegration(project, files, Predicates.<PsiFile>alwaysFalse(),
                                               JetControlFlowDataTraceFactory.EMPTY, CompilerSpecialMode.REGULAR);
    }

    @NotNull
    public static AnalyzeExhaust analyzeFileWithCache(@NotNull final JetFile file,
                                                      @NotNull final Function<JetFile, Collection<JetFile>> declarationProvider) {
        return AnalyzerFacadeWithCache.getInstance(INSTANCE).analyzeFileWithCache(file, declarationProvider);
    }

    @NotNull
    public static AnalyzeExhaust analyzeProjectWithCache(@NotNull final Project project, @NotNull final Collection<JetFile> files) {
        return AnalyzerFacadeWithCache.getInstance(INSTANCE).analyzeProjectWithCache(project, files);
    }
}
