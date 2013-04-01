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
import com.intellij.openapi.application.ApplicationManager;
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
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.plugin.framework.KotlinFrameworkDetector;
import org.jetbrains.jet.plugin.project.TargetPlatform;
import org.jetbrains.k2js.analyze.AnalyzerFacadeForJS;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.config.LibrarySourcesConfig;

import java.util.Collections;

public class KotlinCacheManager {
    public static KotlinCacheManager getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, KotlinCacheManager.class);
    }

    private static final Key<CachedValue<KotlinDeclarationsCache>> KOTLIN_DECLARATIONS_CACHE = Key.create("KOTLIN_DECLARATIONS_CACHE");
    private static final Key<CachedValue<KotlinDeclarationsCache>> KOTLIN_JS_DECLARATIONS_CACHE = Key.create("KOTLIN_JS_DECLARATIONS_CACHE");

    private final Project project;
    private final Object declarationAnalysisLock = new Object();

    private BindingTrace incompleteJvmTrace;

    public KotlinCacheManager(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    private KotlinDeclarationsCache getDeclarations(boolean allowIncomplete) {
        // To prevent dead locks, the lock below must be obtained only inside a read action
        ApplicationManager.getApplication().assertReadAccessAllowed();
        synchronized (declarationAnalysisLock) {
            if (allowIncomplete) {
                if (incompleteJvmTrace != null) {
                    return new KotlinDeclarationsCache() {
                        @NotNull
                        @Override
                        public BindingContext getBindingContext() {
                            return incompleteJvmTrace.getBindingContext();
                        }
                    };
                }
            }

            return CachedValuesManager.getManager(project).getCachedValue(
                    project,
                    KOTLIN_DECLARATIONS_CACHE,
                    new KotlinDeclarationsJvmCacheProvider(),
                    false
            );
        }
    }

    @NotNull
    private KotlinDeclarationsCache getJsDeclarations() {
        // To prevent dead locks, the lock below must be obtained only inside a read action
        ApplicationManager.getApplication().assertReadAccessAllowed();
        synchronized (declarationAnalysisLock) {
            return CachedValuesManager.getManager(project).getCachedValue(
                    project,
                    KOTLIN_JS_DECLARATIONS_CACHE,
                    new KotlinDeclarationsJsCacheProvider(),
                    false
            );
        }
    }

    /**
     * Should be called under read lock.
     */
    @NotNull
    public KotlinDeclarationsCache getDeclarationsFromProject(TargetPlatform platform) {
        return platform == TargetPlatform.JVM ? getDeclarations(false) : getJsDeclarations();
    }

    @NotNull
    public KotlinDeclarationsCache getPossiblyIncompleteDeclarationsForLightClassGeneration() {
        /*
         * If we have the following classes
         *
         *     class A // Kotlin
         *     class B extends A {} // Java
         *     class C : B() // Kotlin
         *
         *  The analysis runs into infinite recursion, because
         *      C needs all members of B (to compute overrides),
         *      and B needs all members of A,
         *      and A is not available from KotlinCacheManager.getDeclarationsFromProject() -- it is being computed right now,
         *      so the analysis runs again...
         *
         *  Our workaround is to return partially complete results when we generate light classes
         */
        return getDeclarations(true);
    }

    private class KotlinDeclarationsJsCacheProvider implements CachedValueProvider<KotlinDeclarationsCache> {
        @Nullable
        @Override
        public Result<KotlinDeclarationsCache> compute() {
            // This lock is already acquired by the calling method,
            // but we put it here to guard for the case of further modifications
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
    }

    private class KotlinDeclarationsJvmCacheProvider implements CachedValueProvider<KotlinDeclarationsCache> {
        @Nullable
        @Override
        public Result<KotlinDeclarationsCache> compute() {
            // This lock is already acquired by the calling method,
            // but we put it here to guard for the case of further modifications
            synchronized (declarationAnalysisLock) {
                BindingTraceContext trace = new BindingTraceContext();

                incompleteJvmTrace = trace;
                AnalyzeExhaust analyzeExhaust;
                try {
                    analyzeExhaust = AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                            project,
                            JetFilesProvider.getInstance(project).allInScope(GlobalSearchScope.allScope(project)),
                            trace,
                            Collections.<AnalyzerScriptParameter>emptyList(),
                            Predicates.<PsiFile>alwaysFalse(),
                            true);
                }
                finally {
                    incompleteJvmTrace = null;
                }

                return Result.<KotlinDeclarationsCache>create(
                        new KotlinDeclarationsCacheImpl(analyzeExhaust),
                        PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT
                );
            }
        }
    }
}
