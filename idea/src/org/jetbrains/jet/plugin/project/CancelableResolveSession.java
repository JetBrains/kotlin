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

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.ModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;

import java.util.concurrent.atomic.AtomicLong;

public class CancelableResolveSession implements KotlinCodeAnalyzer, ModificationTracker {
    private final Object createdForObject;
    private final ResolveSession resolveSession;
    private final ResolveElementCache resolveElementCache;
    private final AtomicLong canceledTracker = new AtomicLong();

    public CancelableResolveSession(@NotNull JetFile file, @NotNull ResolveSession resolveSession) {
        this(file, file.getProject(), resolveSession);
    }

    public CancelableResolveSession(@NotNull Project project, @NotNull ResolveSession resolveSession) {
        this(project, project, resolveSession);
    }

    private CancelableResolveSession(Object createdForObject, Project project, ResolveSession resolveSession) {
        this.createdForObject = createdForObject;
        this.resolveSession = resolveSession;
        this.resolveElementCache = new ResolveElementCache(resolveSession, project);
    }

    @NotNull
    public BindingContext resolveToElement(final JetElement element) {
        return computableWithProcessingCancel(new Computable<BindingContext>() {
            @Override
            public BindingContext compute() {
                return resolveElementCache.resolveElement(element);
            }
        });
    }

    @Override
    public ModuleDescriptor getModuleDescriptor() {
        return computableWithProcessingCancel(new Computable<ModuleDescriptor>() {
            @Override
            public ModuleDescriptor compute() {
                return resolveSession.getModuleDescriptor();
            }
        });
    }

    @NotNull
    @Override
    public ClassDescriptor getClassDescriptor(@NotNull final JetClassOrObject classOrObject) {
        return computableWithProcessingCancel(new Computable<ClassDescriptor>() {
            @Override
            public ClassDescriptor compute() {
                return resolveSession.getClassDescriptor(classOrObject);
            }
        });
    }

    @NotNull
    @Override
    public BindingContext getBindingContext() {
        return computableWithProcessingCancel(new Computable<BindingContext>() {
            @Override
            public BindingContext compute() {
                return resolveSession.getBindingContext();
            }
        });
    }

    @NotNull
    @Override
    public DeclarationDescriptor resolveToDescriptor(final JetDeclaration declaration) {
        return computableWithProcessingCancel(new Computable<DeclarationDescriptor>() {
            @Override
            public DeclarationDescriptor compute() {
                return resolveSession.resolveToDescriptor(declaration);
            }
        });
    }

    @Override
    public void forceResolveAll() {
        computableWithProcessingCancel(new Computable<Integer>() {
            @Override
            public Integer compute() {
                resolveSession.forceResolveAll();
                return 0;
            }
        });
    }

    private <T> T computableWithProcessingCancel(Computable<T> computable) {
        try {
            return computable.compute();
        }
        catch (ProcessCanceledException canceledException) {
            canceledTracker.getAndIncrement();
            throw canceledException;
        }
    }

    @Override
    public long getModificationCount() {
        return canceledTracker.get();
    }

    @Override
    public String toString() {
        return "CancelableResolveSession: " + getModificationCount() + " " + createdForObject + " " + createdForObject.hashCode();
    }
}
