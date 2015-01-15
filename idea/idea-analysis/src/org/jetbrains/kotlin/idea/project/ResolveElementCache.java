/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.project;

import com.intellij.openapi.project.Project;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.stubindex.JetProbablyNothingFunctionShortNameIndex;
import org.jetbrains.kotlin.idea.stubindex.JetProbablyNothingPropertyShortNameIndex;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.AdditionalCheckerProvider;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.*;
import org.jetbrains.kotlin.storage.LazyResolveStorageManager;
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ResolveElementCache extends ElementResolver {
    private final Project project;
    private final CachedValue<MemoizedFunctionToNotNull<JetElement, BindingContext>> additionalResolveCache;

    public ResolveElementCache(ResolveSession resolveSession, Project project) {
        super(resolveSession);
        this.project = project;

        // Recreate internal cache after change of modification count
        this.additionalResolveCache =
                CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<MemoizedFunctionToNotNull<JetElement, BindingContext>>() {
                            @Nullable
                            @Override
                            public Result<MemoizedFunctionToNotNull<JetElement, BindingContext>> compute() {
                                ResolveSession resolveSession = ResolveElementCache.this.resolveSession;
                                LazyResolveStorageManager manager = resolveSession.getStorageManager();
                                MemoizedFunctionToNotNull<JetElement, BindingContext> elementsCacheFunction =
                                        manager.createSoftlyRetainedMemoizedFunction(new Function1<JetElement, BindingContext>() {
                                            @Override
                                            public BindingContext invoke(JetElement jetElement) {
                                                return elementAdditionalResolve(jetElement, jetElement, BodyResolveMode.FULL);
                                            }
                                        });

                                return Result.create(elementsCacheFunction,
                                                     PsiModificationTracker.MODIFICATION_COUNT,
                                                     resolveSession.getExceptionTracker());
                            }
                        },
                        false);
    }

    @NotNull
    @Override
    public BindingContext getElementAdditionalResolve(@NotNull JetElement jetElement) {
        return additionalResolveCache.getValue().invoke(jetElement);
    }

    @NotNull
    @Override
    public AdditionalCheckerProvider getAdditionalCheckerProvider(@NotNull JetFile jetFile) {
        return TargetPlatformDetector.getPlatform(jetFile).getAdditionalCheckerProvider();
    }

    @NotNull
    @Override
    protected ProbablyNothingCallableNames probablyNothingCallableNames() {
        return new ProbablyNothingCallableNames() {
            @NotNull
            @Override
            public Set<String> functionNames() {
                // we have to add hardcoded-names until we have Kotlin compiled classes in caches
                Set<String> hardcodedNames = DefaultNothingCallableNames.INSTANCE$.functionNames();
                Collection<String> indexedNames = JetProbablyNothingFunctionShortNameIndex.getInstance().getAllKeys(project);
                Set<String> set = new HashSet<String>(hardcodedNames.size() + indexedNames.size());
                set.addAll(hardcodedNames);
                set.addAll(indexedNames);
                return set;
            }

            @NotNull
            @Override
            public Set<String> propertyNames() {
                return new HashSet<String>(JetProbablyNothingPropertyShortNameIndex.getInstance().getAllKeys(project));
            }
        };
    }
}
