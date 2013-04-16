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

package org.jetbrains.jet.analyzer;

import com.google.common.base.Predicate;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.di.InjectorForBodyResolve;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.resolve.*;

import java.util.List;

public class AnalyzerFacadeForEverything {

    private AnalyzerFacadeForEverything() {
    }

    public static AnalyzeExhaust analyzeBodiesInFilesWithJavaIntegration(
            Project project, List<AnalyzerScriptParameter> scriptParameters, Predicate<PsiFile> filesToAnalyzeCompletely,
            @NotNull BindingTrace traceContext,
            @NotNull BodiesResolveContext bodiesResolveContext,
            @NotNull ModuleDescriptor module) {

        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(
                filesToAnalyzeCompletely, false, false, scriptParameters);

        bodiesResolveContext.setTopDownAnalysisParameters(topDownAnalysisParameters);

        InjectorForBodyResolve injector = new InjectorForBodyResolve(
                project, topDownAnalysisParameters,
                new ObservableBindingTrace(traceContext),
                bodiesResolveContext, module);

        try {
            injector.getBodyResolver().resolveBodies();
            return AnalyzeExhaust.success(traceContext.getBindingContext(), module);
        } finally {
            injector.destroy();
        }
    }

}
