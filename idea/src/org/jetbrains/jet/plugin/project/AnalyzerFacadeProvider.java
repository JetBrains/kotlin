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

package org.jetbrains.jet.plugin.project;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzerFacade;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.k2js.analyze.AnalyzerFacadeForJS;

/**
 * @author Pavel Talanov
 */
public final class AnalyzerFacadeProvider {

    private AnalyzerFacadeProvider() {
    }

    @NotNull
    public static AnalyzerFacade getAnalyzerFacadeForFile(@NotNull JetFile file) {
        if (JsModuleDetector.isJsProject(file.getProject())) {
            return AnalyzerFacadeForJS.INSTANCE;
        }
        return AnalyzerFacadeForJVM.INSTANCE;
    }

    @NotNull
    public static AnalyzerFacade getAnalyzerFacadeForProject(@NotNull Project project) {
        if (JsModuleDetector.isJsProject(project)) {
            return AnalyzerFacadeForJS.INSTANCE;
        }
        return AnalyzerFacadeForJVM.INSTANCE;
    }
}
