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

import com.google.common.base.Predicates;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.BodiesResolveContext;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Pavel Talanov
 */
public final class AnalyzerFacadeWithCache {

    private static final Logger LOG = Logger.getInstance("org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache");

    private final static Key<CachedValue<AnalyzeExhaust>> ANALYZE_EXHAUST_HEADERS = Key.create("ANALYZE_EXHAUST_HEADERS");
    private final static Key<CachedValue<AnalyzeExhaust>> ANALYZE_EXHAUST_FULL = Key.create("ANALYZE_EXHAUST_FULL");

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
     *
     * @param file
     * @param declarationProvider
     * @return
     */
    // TODO: Also need to pass several files when user have multi-file environment
    @NotNull
    public static AnalyzeExhaust analyzeFileWithCache(@NotNull final JetFile file,
                                                      @NotNull final Function<JetFile, Collection<JetFile>> declarationProvider) {
        // Need lock for getValue(), because parallel threads can start evaluation of compute() simultaneously
        synchronized (lock) {
            CachedValue<AnalyzeExhaust> bindingContextCachedValue = file.getUserData(ANALYZE_EXHAUST_FULL);
            if (bindingContextCachedValue == null) {
                bindingContextCachedValue =
                        CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<AnalyzeExhaust>() {
                            @Override
                            public Result<AnalyzeExhaust> compute() {
                                try {
                                    // System.out.println("===============ReCache - In-Block==============");

                                    // Collect context for headers first
                                    Collection<JetFile> allFilesToAnalyze = declarationProvider.fun(file);
                                    AnalyzeExhaust analyzeExhaustHeaders = analyzeHeadersWithCacheOnFile(file, allFilesToAnalyze);

                                    BodiesResolveContext context = analyzeExhaustHeaders.getBodiesResolveContext();
                                    assert context != null : "Headers resolver should prepare and stored information for bodies resolve";

                                    // Need to resolve bodies in given file
                                    AnalyzeExhaust exhaust = AnalyzerFacadeProvider.getAnalyzerFacadeForFile(file).analyzeBodiesInFiles(
                                            file.getProject(),
                                            Predicates.<PsiFile>equalTo(file),
                                            JetControlFlowDataTraceFactory.EMPTY,
                                            new DelegatingBindingTrace(analyzeExhaustHeaders.getBindingContext()),
                                            context);

                                    return new Result<AnalyzeExhaust>(exhaust, PsiModificationTracker.MODIFICATION_COUNT);
                                }
                                catch (ProcessCanceledException e) {
                                    throw e;
                                }
                                catch (Throwable e) {
                                    handleError(e);
                                    return emptyExhaustWithDiagnosticOnFile(e);
                                }
                            }

                            @NotNull
                            private Result<AnalyzeExhaust> emptyExhaustWithDiagnosticOnFile(Throwable e) {
                                BindingTraceContext bindingTraceContext = new BindingTraceContext();
                                bindingTraceContext.report(Errors.EXCEPTION_WHILE_ANALYZING.on(file, e));
                                AnalyzeExhaust analyzeExhaust = AnalyzeExhaust.error(bindingTraceContext.getBindingContext(), e);
                                return new Result<AnalyzeExhaust>(analyzeExhaust, PsiModificationTracker.MODIFICATION_COUNT);
                            }
                        }, false);

                file.putUserData(ANALYZE_EXHAUST_FULL, bindingContextCachedValue);
            }

            return bindingContextCachedValue.getValue();
        }
    }

    private static AnalyzeExhaust analyzeHeadersWithCacheOnFile(@NotNull final JetFile fileToCache,
                                                                @NotNull final Collection<JetFile> headerFiles) {
        CachedValue<AnalyzeExhaust> bindingContextCachedValue = fileToCache.getUserData(ANALYZE_EXHAUST_HEADERS);
        if (bindingContextCachedValue == null) {
            bindingContextCachedValue =
                    CachedValuesManager.getManager(fileToCache.getProject()).createCachedValue(new CachedValueProvider<AnalyzeExhaust>() {
                        @Override
                        public Result<AnalyzeExhaust> compute() {
                            // System.out.println("===============ReCache - OUT-OF-BLOCK==============");
                            AnalyzeExhaust exhaust = AnalyzerFacadeProvider.getAnalyzerFacadeForFile(fileToCache)
                                    .analyzeFiles(fileToCache.getProject(),
                                                  headerFiles,
                                                  Predicates.<PsiFile>alwaysFalse(),
                                                  JetControlFlowDataTraceFactory.EMPTY);

                            return new Result<AnalyzeExhaust>(exhaust, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
                        }
                    }, false);
            fileToCache.putUserData(ANALYZE_EXHAUST_HEADERS, bindingContextCachedValue);
        }

        return bindingContextCachedValue.getValue();
    }

    private static void handleError(@NotNull Throwable e) {
        DiagnosticUtils.throwIfRunningOnServer(e);
        LOG.error(e);
    }
}
