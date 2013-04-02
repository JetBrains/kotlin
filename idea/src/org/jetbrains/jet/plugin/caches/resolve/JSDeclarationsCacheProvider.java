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

package org.jetbrains.jet.plugin.caches.resolve;

import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.plugin.framework.KotlinFrameworkDetector;
import org.jetbrains.jet.plugin.project.TargetPlatform;
import org.jetbrains.k2js.analyze.AnalyzerFacadeForJS;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.config.LibrarySourcesConfig;

class JSDeclarationsCacheProvider extends DeclarationsCacheProvider {
    private final CachedValueProvider<KotlinDeclarationsCache> declarationsProvider;
    private final Key<CachedValue<KotlinDeclarationsCache>> cachedKey;
    private final Object declarationAnalysisLock = new Object();

    JSDeclarationsCacheProvider(final Project project) {
        super(project, TargetPlatform.JS);

        cachedKey = Key.create("KOTLIN_JS_DECLARATIONS_CACHE");

        declarationsProvider = new CachedValueProvider<KotlinDeclarationsCache>() {
            @Nullable
            @Override
            public Result<KotlinDeclarationsCache> compute() {
                synchronized (declarationAnalysisLock) {
                    LibrarySourcesConfig config = new LibrarySourcesConfig(
                            project, "default",
                            KotlinFrameworkDetector.getLibLocationAndTargetForProject(project).first,
                            EcmaVersion.defaultVersion());

                    AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForJS.analyzeFiles(
                            JetFilesProvider.getInstance(project).allInScope(GlobalSearchScope.allScope(project)),
                            Predicates.<PsiFile>alwaysFalse(),
                            config,
                            true);

                    return Result.<KotlinDeclarationsCache>create(
                            new KotlinDeclarationsCacheImpl(analyzeExhaust),
                            PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT
                    );
                }
            }
        };
    }

    @Override
    public KotlinDeclarationsCache getDeclarations(boolean allowIncomplete) {
        synchronized (declarationAnalysisLock) {
            return CachedValuesManager.getManager(project).getCachedValue(
                    project,
                    cachedKey,
                    declarationsProvider,
                    false
            );
        }
    }
}
