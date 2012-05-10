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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;

import static org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache.analyzeFileWithCache;

/**
 * @author abreslav
 */
public final class WholeProjectAnalyzerFacade {

    /**
     * Forbid creating
     */
    private WholeProjectAnalyzerFacade() {
    }

    @NotNull
    public static AnalyzeExhaust analyzeProjectWithCacheOnAFile(@NotNull JetFile file) {
        return analyzeFileWithCache(file, JetFilesProvider.getInstance(file.getProject()).sampleToAllFilesInModule());
    }
}
