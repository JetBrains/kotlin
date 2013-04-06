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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiModificationTrackerImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.Function;
import com.intellij.util.containers.SLRUCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.plugin.caches.resolve.KotlinDeclarationsCacheImpl;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManager;
import org.jetbrains.jet.plugin.caches.resolve.KotlinDeclarationsCache;
import org.jetbrains.jet.plugin.util.ApplicationUtils;

import java.util.Collection;
import java.util.Collections;

public final class AnalyzerFacadeWithCache {

    private static final Logger LOG = Logger.getInstance("org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache");

    private final static Key<CachedValue<SLRUCache<JetFile, AnalyzeExhaust>>> ANALYZE_EXHAUST_FULL = Key.create("ANALYZE_EXHAUST_FULL");

    private static final Object lock = new Object();
    public static final Function<JetFile, Collection<JetFile>> SINGLE_DECLARATION_PROVIDER = new Function<JetFile, Collection<JetFile>>() {
        @Override
        public Collection<JetFile> fun(JetFile file) {
            return Collections.singleton(file);
        }
    };

    private AnalyzerFacadeWithCache() {
    }

    /**
     * Analyze project with string cache for given file. Given file will be fully analyzed.
     */
    // TODO: Also need to pass several files when user have multi-file environment
    @NotNull
    public static AnalyzeExhaust analyzeFileWithCache(@NotNull JetFile file) {
        // Need lock, because parallel threads can start evaluation of compute() simultaneously
        synchronized (lock) {
            Project project = file.getProject();
            return CachedValuesManager.getManager(project).getCachedValue(
                    project,
                    ANALYZE_EXHAUST_FULL,
                    new CachedValueProvider<SLRUCache<JetFile, AnalyzeExhaust>>() {
                        @Nullable
                        @Override
                        public Result<SLRUCache<JetFile, AnalyzeExhaust>> compute() {
                            SLRUCache<JetFile, AnalyzeExhaust> cache = new SLRUCache<JetFile, AnalyzeExhaust>(3, 8) {

                                @NotNull
                                @Override
                                public AnalyzeExhaust createValue(JetFile file) {
                                    try {
                                        if (DumbService.isDumb(file.getProject())) {
                                            return emptyExhaust();
                                        }

                                        ApplicationUtils.warnTimeConsuming(LOG);

                                        AnalyzeExhaust analyzeExhaustHeaders = analyzeHeadersWithCacheOnFile(file);

                                        return analyzeBodies(analyzeExhaustHeaders, file);
                                    }
                                    catch (ProcessCanceledException e) {
                                        throw e;
                                    }
                                    catch (Throwable e) {
                                        handleError(e);
                                        return emptyExhaustWithDiagnosticOnFile(file, e);
                                    }
                                }
                            };
                            return Result.create(cache, PsiModificationTracker.MODIFICATION_COUNT);
                        }
                    },
                    false
            ).get(file);
        }
    }

    private static AnalyzeExhaust emptyExhaust() {
        return AnalyzeExhaust.success(BindingContext.EMPTY, ModuleConfiguration.EMPTY);
    }

    private static AnalyzeExhaust analyzeHeadersWithCacheOnFile(@NotNull JetFile fileToCache) {
        KotlinDeclarationsCache cache = KotlinCacheManager.getInstance(fileToCache.getProject()).getDeclarationsFromProject();
        return ((KotlinDeclarationsCacheImpl) cache).getAnalyzeExhaust();
    }

    private static AnalyzeExhaust analyzeBodies(AnalyzeExhaust analyzeExhaustHeaders, JetFile file) {
        BodiesResolveContext context = analyzeExhaustHeaders.getBodiesResolveContext();
        ModuleConfiguration moduleConfiguration = analyzeExhaustHeaders.getModuleConfiguration();
        assert context != null : "Headers resolver should prepare and stored information for bodies resolve";

        // Need to resolve bodies in given file and all in the same package
        return AnalyzerFacadeProvider.getAnalyzerFacadeForFile(file).analyzeBodiesInFiles(
                file.getProject(),
                Collections.<AnalyzerScriptParameter>emptyList(),
                new JetFilesProvider.SameJetFilePredicate(file),
                new DelegatingBindingTrace(analyzeExhaustHeaders.getBindingContext(),
                                           "trace to resolve bodies in file", file.getName()),
                context,
                moduleConfiguration);
    }

    @NotNull
    private static AnalyzeExhaust emptyExhaustWithDiagnosticOnFile(JetFile file, Throwable e) {
        BindingTraceContext bindingTraceContext = new BindingTraceContext();
        bindingTraceContext.report(Errors.EXCEPTION_WHILE_ANALYZING.on(file, e));
        AnalyzeExhaust analyzeExhaust = AnalyzeExhaust.error(bindingTraceContext.getBindingContext(), e);

        // Force invalidating of headers cache - temp decision for monitoring rewrite slice bug
        PsiModificationTracker tracker = PsiManager.getInstance(file.getProject()).getModificationTracker();
        ((PsiModificationTrackerImpl) tracker).incOutOfCodeBlockModificationCounter();

        return analyzeExhaust;
    }

    private static void handleError(@NotNull Throwable e) {
        DiagnosticUtils.throwIfRunningOnServer(e);
        LOG.error(e);
    }

    @NotNull
    public static ResolveSession getLazyResolveSession(@NotNull JetFile file) {
        Project fileProject = file.getProject();

        Collection<JetFile> files = JetFilesProvider.getInstance(fileProject).allInScope(GlobalSearchScope.allScope(fileProject));

        // Given file can differ from the original because it can be a virtual copy with some modifications
        JetFile originalFile = (JetFile) file.getOriginalFile();
        files.remove(originalFile);
        files.add(file);

        return AnalyzerFacadeProvider.getAnalyzerFacadeForFile(file).getLazyResolveSession(fileProject, files);
    }
}
