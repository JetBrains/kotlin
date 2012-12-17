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

package org.jetbrains.jet.plugin.caches.resolve;

import com.google.common.base.Predicates;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;

import java.util.Collections;

public class KotlinCacheManager {

    public static KotlinCacheManager getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, KotlinCacheManager.class);
    }

    private final Key<CachedValue<KotlinDeclarationsCache>> KOTLIN_DECLARATIONS_CACHE = Key.create("KOTLIN_DECLARATIONS_CACHE");

    private final Project project;
    private final Object declarationAnalysisLock = new Object();


    public KotlinCacheManager(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public KotlinDeclarationsCache getDeclarationsFromProject(@NotNull Project project) {
        synchronized (declarationAnalysisLock) {
            return CachedValuesManager.getManager(project).getCachedValue(
                    project,
                    KOTLIN_DECLARATIONS_CACHE,
                    new KotlinDeclarationsCacheProvider(),
                    false
            );
        }
    }

    private class KotlinDeclarationsCacheProvider implements CachedValueProvider<KotlinDeclarationsCache> {
        @Nullable
        @Override
        public Result<KotlinDeclarationsCache> compute() {
            AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForJVM.INSTANCE.analyzeFiles(
                    project,
                    JetFilesProvider.getInstance(project).allInScope(GlobalSearchScope.allScope(project)),
                    Collections.<AnalyzerScriptParameter>emptyList(),
                    Predicates.<PsiFile>alwaysFalse()
            );
            return Result.<KotlinDeclarationsCache>create(
                    new AnalyzeExhaustAsKotlinDeclarationsCache(analyzeExhaust),
                    PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT
            );
        }

    }
}
