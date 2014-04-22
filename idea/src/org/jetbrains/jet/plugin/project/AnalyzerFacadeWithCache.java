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

package org.jetbrains.jet.plugin.project;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.containers.SLRUCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.analyzer.AnalyzerFacade;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.plugin.caches.resolve.DeclarationsCacheProvider;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManager;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;

import java.util.Collection;
import java.util.HashSet;

public final class AnalyzerFacadeWithCache {

    private AnalyzerFacadeWithCache() {
    }

    /**
     * Analyze project with string cache for given file. Given file will be fully analyzed.
     */
    // TODO: Also need to pass several files when user have multi-file environment
    @NotNull
    public static AnalyzeExhaust analyzeFileWithCache(@NotNull JetFile file) {
        return ResolvePackage.getAnalysisResults(file);
    }

    @NotNull
    public static BindingContext getContextForElement(@NotNull JetElement jetElement) {
        ResolveSessionForBodies resolveSessionForBodies = getLazyResolveSessionForFile(jetElement.getContainingJetFile());
        return resolveSessionForBodies.resolveToElement(jetElement);
    }

    @NotNull
    public static ResolveSessionForBodies getLazyResolveSessionForFile(@NotNull JetFile file) {
        Project project = file.getProject();
        DeclarationsCacheProvider provider = KotlinCacheManager.getInstance(project).getRegisteredProvider(TargetPlatformDetector.getPlatform(file));

        if (!provider.areDeclarationsAvailable(file)) {
            // There can be request for temp files (in completion) or non-source (in library) files. Create temp sessions for them.
            CachedValue<ResolveSessionForBodies> cachedValue;

            synchronized (PER_FILE_SESSION_CACHE) {
                cachedValue = PER_FILE_SESSION_CACHE.get(file);
            }

            return cachedValue.getValue();
        }

        return provider.getLazyResolveSession();
    }

    private static final SLRUCache<JetFile, CachedValue<ResolveSessionForBodies>> PER_FILE_SESSION_CACHE = new SLRUCache<JetFile, CachedValue<ResolveSessionForBodies>>(2, 3) {
        @NotNull
        @Override
        public CachedValue<ResolveSessionForBodies> createValue(final JetFile file) {
            final Project fileProject = file.getProject();
            return CachedValuesManager.getManager(fileProject).createCachedValue(
                    // Each value monitors OUT_OF_CODE_BLOCK_MODIFICATION_COUNT and modification tracker of the stored value
                    new CachedValueProvider<ResolveSessionForBodies>() {
                        @Nullable
                        @Override
                        public Result<ResolveSessionForBodies> compute() {
                            Project project = file.getProject();


                            Collection<JetFile> files = new HashSet<JetFile>(JetFilesProvider.getInstance(project).allInScope(GlobalSearchScope.allScope(project)));

                            // Add requested file to the list of files for searching declarations
                            files.add(file);

                            if (file != file.getOriginalFile()) {
                                // Given file can be a non-physical copy of the file in list (completion case). Remove the prototype file.

                                //noinspection SuspiciousMethodCalls
                                files.remove(file.getOriginalFile());
                            }

                            AnalyzerFacade facade = AnalyzerFacadeProvider.getAnalyzerFacadeForFile(file);
                            ResolveSession resolveSession = facade.createSetup(fileProject, files).getLazyResolveSession();
                            return Result.create(new ResolveSessionForBodies(file, resolveSession), PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
                        }
                    },
                    true);
        }
    };
}
