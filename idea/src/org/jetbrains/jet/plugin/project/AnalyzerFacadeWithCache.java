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
import com.intellij.openapi.project.Project;
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

import java.util.Collection;
import java.util.Collections;

/**
 * @author Pavel Talanov
 */
public final class AnalyzerFacadeWithCache {

    private static final Logger LOG = Logger.getInstance("org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache");

    private final static Key<CachedValue<AnalyzeExhaust>> ANALYZE_EXHAUST = Key.create("ANALYZE_EXHAUST");
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
    @NotNull
    public static AnalyzeExhaust analyzeFileWithCache(@NotNull final JetFile file,
                                                      @NotNull final Function<JetFile, Collection<JetFile>> declarationProvider) {
        // Need lock for getValue(), because parallel threads can start evaluation of compute() simultaneously
        synchronized (lock) {
            CachedValue<AnalyzeExhaust> bindingContextCachedValue = file.getUserData(ANALYZE_EXHAUST);
            if (bindingContextCachedValue == null) {
                bindingContextCachedValue =
                    CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<AnalyzeExhaust>() {
                        @Override
                        public Result<AnalyzeExhaust> compute() {
                            try {
                                AnalyzeExhaust exhaust = AnalyzerFacadeProvider.getAnalyzerFacadeForFile(file)
                                    .analyzeFiles(file.getProject(),
                                                  declarationProvider.fun(file),
                                                  Predicates.<PsiFile>equalTo(file),
                                                  JetControlFlowDataTraceFactory.EMPTY);
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
                            AnalyzeExhaust analyzeExhaust = new AnalyzeExhaust(bindingTraceContext.getBindingContext(), null);
                            return new Result<AnalyzeExhaust>(analyzeExhaust, PsiModificationTracker.MODIFICATION_COUNT);
                        }
                    }, false);
                file.putUserData(ANALYZE_EXHAUST, bindingContextCachedValue);
            }
            return bindingContextCachedValue.getValue();
        }
    }

    private static void handleError(@NotNull Throwable e) {
        DiagnosticUtils.throwIfRunningOnServer(e);
        LOG.error(e);
    }

    /**
     * Analyze project with string cache for the whole project. All given files will be analyzed only for descriptors.
     */
    @NotNull
    public static AnalyzeExhaust analyzeProjectWithCache(@NotNull final Project project, @NotNull final Collection<JetFile> files) {
        // Need lock for getValue(), because parallel threads can start evaluation of compute() simultaneously
        synchronized (lock) {
            CachedValue<AnalyzeExhaust> bindingContextCachedValue = project.getUserData(ANALYZE_EXHAUST);
            if (bindingContextCachedValue == null) {
                bindingContextCachedValue =
                    CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<AnalyzeExhaust>() {
                        @Override
                        public Result<AnalyzeExhaust> compute() {
                            try {
                                AnalyzeExhaust analyzeExhaust = AnalyzerFacadeProvider.getAnalyzerFacadeForProject(project)
                                    .analyzeFiles(project,
                                                  files,
                                                  Predicates.<PsiFile>alwaysFalse(),
                                                  JetControlFlowDataTraceFactory.EMPTY);
                                return new Result<AnalyzeExhaust>(analyzeExhaust, PsiModificationTracker.MODIFICATION_COUNT);
                            }
                            catch (ProcessCanceledException e) {
                                throw e;
                            }
                            catch (Throwable e) {
                                handleError(e);
                                return emptyExhaust();
                            }
                        }

                        @NotNull
                        private Result<AnalyzeExhaust> emptyExhaust() {
                            BindingTraceContext bindingTraceContext = new BindingTraceContext();
                            AnalyzeExhaust analyzeExhaust = new AnalyzeExhaust(bindingTraceContext.getBindingContext(), null);
                            return new Result<AnalyzeExhaust>(analyzeExhaust, PsiModificationTracker.MODIFICATION_COUNT);
                        }
                    }, false);
                project.putUserData(ANALYZE_EXHAUST, bindingContextCachedValue);
            }
            return bindingContextCachedValue.getValue();
        }
    }
}
