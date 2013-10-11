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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.plugin.project.TargetPlatform;

import java.util.Collections;

class JvmDeclarationsCacheProvider extends DeclarationsCacheProvider {
    private final CachedValueProvider<KotlinDeclarationsCache> declarationsProvider;
    private final Key<CachedValue<KotlinDeclarationsCache>> cachedKey;
    private final Object declarationAnalysisLock = new Object();

    private BindingTrace incompleteTrace;

    JvmDeclarationsCacheProvider(final Project project) {
        super(project, TargetPlatform.JVM);

        cachedKey = Key.create("KOTLIN_JVM_DECLARATIONS_CACHE");

        declarationsProvider = new CachedValueProvider<KotlinDeclarationsCache>() {
            @Nullable
            @Override
            public Result<KotlinDeclarationsCache> compute() {
                // This lock is already acquired by the calling method,
                // but we put it here to guard for the case of further modifications
                synchronized (declarationAnalysisLock) {
                    incompleteTrace = new BindingTraceContext();

                    AnalyzeExhaust analyzeExhaust;
                    try {
                        analyzeExhaust = AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                                project,
                                JetFilesProvider.getInstance(project).allInScope(GlobalSearchScope.allScope(project)),
                                incompleteTrace,
                                Collections.<AnalyzerScriptParameter>emptyList(),
                                Predicates.<PsiFile>alwaysFalse(),
                                true);
                    }
                    finally {
                        incompleteTrace = null;
                    }

                    return Result.<KotlinDeclarationsCache>create(
                            new KotlinDeclarationsCacheImpl(analyzeExhaust),
                            PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT,
                            KotlinCacheManager.getInstance(project).getDeclarationsTracker()
                    );
                }
            }
        };
    }

    @Override
    @NotNull
    public KotlinDeclarationsCache getDeclarations(boolean allowIncomplete) {
        synchronized (declarationAnalysisLock) {
            if (allowIncomplete) {
                if (incompleteTrace != null) {
                    // saving context to local variable to avoid race condition
                    final BindingContext context = incompleteTrace.getBindingContext();
                    return new KotlinDeclarationsCache() {
                        @NotNull
                        @Override
                        public BindingContext getBindingContext() {
                            return context;
                        }
                    };
                }
            }

            return CachedValuesManager.getManager(project).getCachedValue(
                    project,
                    cachedKey,
                    declarationsProvider,
                    false
            );
        }
    }
}
