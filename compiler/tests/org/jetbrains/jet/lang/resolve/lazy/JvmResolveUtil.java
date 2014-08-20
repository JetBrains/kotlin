/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collection;
import java.util.Collections;

public class JvmResolveUtil {
    @NotNull
    public static AnalyzeExhaust analyzeOneFileWithJavaIntegrationAndCheckForErrors(@NotNull JetFile file) {
        AnalyzingUtils.checkForSyntacticErrors(file);

        AnalyzeExhaust analyzeExhaust = analyzeOneFileWithJavaIntegration(file);

        AnalyzingUtils.throwExceptionOnErrors(analyzeExhaust.getBindingContext());

        return analyzeExhaust;
    }

    @NotNull
    public static AnalyzeExhaust analyzeOneFileWithJavaIntegration(@NotNull JetFile file) {
        return analyzeFilesWithJavaIntegration(file.getProject(), Collections.singleton(file),
                                               Predicates.<PsiFile>alwaysTrue());
    }

    @NotNull
    public static AnalyzeExhaust analyzeFilesWithJavaIntegrationAndCheckForErrors(
            @NotNull Project project,
            @NotNull Collection<JetFile> files,
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely
    ) {
        for (JetFile file : files) {
            AnalyzingUtils.checkForSyntacticErrors(file);
        }

        AnalyzeExhaust analyzeExhaust = analyzeFilesWithJavaIntegration(project, files, filesToAnalyzeCompletely);

        AnalyzingUtils.throwExceptionOnErrors(analyzeExhaust.getBindingContext());

        return analyzeExhaust;
    }

    @NotNull
    public static AnalyzeExhaust analyzeFilesWithJavaIntegration(
            @NotNull Project project,
            @NotNull Collection<JetFile> files,
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely
    ) {
        BindingTraceContext bindingTraceContext = new BindingTraceContext();

        ModuleDescriptorImpl module = AnalyzerFacadeForJVM.createJavaModule("<module>");
        module.addDependencyOnModule(module);
        module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
        module.seal();
        CliLightClassGenerationSupport lightClassGenerationSupport = CliLightClassGenerationSupport.getInstanceForCli(project);
        if (lightClassGenerationSupport != null) {
            lightClassGenerationSupport.setModule(module);
        }
        return AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(project, files, bindingTraceContext, filesToAnalyzeCompletely,
                                                                    module,
                                                                    null, null);
    }
}
