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

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeProvider;
import org.jetbrains.jet.plugin.project.CancelableResolveSession;
import org.jetbrains.jet.plugin.project.TargetPlatform;

import java.util.Collection;

public abstract class DeclarationsCacheProvider {
    private final CachedValue<CancelableResolveSession> lazyResolveCache;

    protected final TargetPlatform platform;
    protected final Project project;

    DeclarationsCacheProvider(Project project, TargetPlatform platform) {
        this.platform = platform;
        this.project = project;

        this.lazyResolveCache = CachedValuesManager.getManager(project).createCachedValue(
                new CancelableResolveSessionValueProvider(project, platform), true);
    }

    public abstract KotlinDeclarationsCache getDeclarations(boolean allowIncomplete);

    @NotNull
    public CancelableResolveSession getLazyResolveSession() {
        return lazyResolveCache.getValue();
    }

    private static class CancelableResolveSessionValueProvider implements CachedValueProvider<CancelableResolveSession> {
        private final Project project;
        private final TargetPlatform platform;

        private CancelableResolveSessionValueProvider(Project project, TargetPlatform platform) {
            this.project = project;
            this.platform = platform;
        }

        @Nullable
        @Override
        public synchronized Result<CancelableResolveSession> compute() {
            Collection<JetFile> files = JetFilesProvider.getInstance(project).allInScope(GlobalSearchScope.allScope(project));
            ResolveSession resolveSession = AnalyzerFacadeProvider.getAnalyzerFacade(platform).getLazyResolveSession(project, files);
            return Result.create(new CancelableResolveSession(project, resolveSession), PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
        }
    }
}
