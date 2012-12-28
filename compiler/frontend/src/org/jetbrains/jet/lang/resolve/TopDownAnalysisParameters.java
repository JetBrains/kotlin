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

package org.jetbrains.jet.lang.resolve;

import com.google.common.base.Predicate;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Various junk that cannot be placed into context (yet).
 */
public class TopDownAnalysisParameters {
    @NotNull
    private final Predicate<PsiFile> analyzeCompletely;
    private final boolean analyzingBootstrapLibrary;
    private final boolean declaredLocally;
    @NotNull
    private final List<AnalyzerScriptParameter> scriptParameters;

    public TopDownAnalysisParameters(
            @NotNull Predicate<PsiFile> analyzeCompletely,
            boolean analyzingBootstrapLibrary,
            boolean declaredLocally,
            @NotNull List<AnalyzerScriptParameter> scriptParameters) {
        this.analyzeCompletely = analyzeCompletely;
        this.analyzingBootstrapLibrary = analyzingBootstrapLibrary;
        this.declaredLocally = declaredLocally;
        this.scriptParameters = scriptParameters;
    }

    @NotNull
    public Predicate<PsiFile> getAnalyzeCompletely() {
        return analyzeCompletely;
    }

    public boolean isAnalyzingBootstrapLibrary() {
        return analyzingBootstrapLibrary;
    }

    public boolean isDeclaredLocally() {
        return declaredLocally;
    }

    @NotNull
    public List<AnalyzerScriptParameter> getScriptParameters() {
        return scriptParameters;
    }
}
