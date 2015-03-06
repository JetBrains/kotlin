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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.kotlin.context.ContextPackage;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.AnalyzingUtils;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM;

import java.util.Collection;
import java.util.Collections;

public class JvmResolveUtil {
    @NotNull
    public static AnalysisResult analyzeOneFileWithJavaIntegrationAndCheckForErrors(@NotNull JetFile file) {
        AnalyzingUtils.checkForSyntacticErrors(file);

        AnalysisResult analysisResult = analyzeOneFileWithJavaIntegration(file);

        AnalyzingUtils.throwExceptionOnErrors(analysisResult.getBindingContext());

        return analysisResult;
    }

    @NotNull
    public static AnalysisResult analyzeOneFileWithJavaIntegration(@NotNull JetFile file) {
        return analyzeFilesWithJavaIntegration(file.getProject(), Collections.singleton(file)
        );
    }

    @NotNull
    public static AnalysisResult analyzeFilesWithJavaIntegrationAndCheckForErrors(
            @NotNull Project project,
            @NotNull Collection<JetFile> files
    ) {
        for (JetFile file : files) {
            AnalyzingUtils.checkForSyntacticErrors(file);
        }

        AnalysisResult analysisResult = analyzeFilesWithJavaIntegration(project, files);

        AnalyzingUtils.throwExceptionOnErrors(analysisResult.getBindingContext());

        return analysisResult;
    }

    @NotNull
    public static AnalysisResult analyzeFilesWithJavaIntegration(
            @NotNull Project project,
            @NotNull Collection<JetFile> files
    ) {
        ModuleDescriptorImpl module = TopDownAnalyzerFacadeForJVM.createJavaModule("<module>");
        module.addDependencyOnModule(module);
        module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
        module.seal();

        BindingTrace trace = new CliLightClassGenerationSupport.CliBindingTrace();

        return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegrationWithCustomContext(
                project, ContextPackage.GlobalContext(), files, trace, module, null, null);
    }
}
