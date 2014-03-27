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

public class ResolveSessionForBodies implements KotlinCodeAnalyzer, ModificationTracker {
    private final Object createdForObject;
    private final ResolveSession resolveSession;
    private final ResolveElementCache resolveElementCache;

    public ResolveSessionForBodies(@NotNull JetFile file, @NotNull ResolveSession resolveSession) {
        this(file, file.getProject(), resolveSession);
    }

    public ResolveSessionForBodies(@NotNull Project project, @NotNull ResolveSession resolveSession) {
        this(project, project, resolveSession);
    }

    private ResolveSessionForBodies(Object createdForObject, Project project, ResolveSession resolveSession) {
        this.createdForObject = createdForObject;
        this.resolveSession = resolveSession;
        this.resolveElementCache = new ResolveElementCache(resolveSession, project);
    }

    @NotNull
    public BindingContext resolveToElement(JetElement element) {
        return resolveElementCache.resolveToElement(element);
    }

    @NotNull
    @Override
    public ModuleDescriptor getModuleDescriptor() {
        return resolveSession.getModuleDescriptor();
    }

    @NotNull
    @Override
    public ClassDescriptor getClassDescriptor(@NotNull JetClassOrObject classOrObject) {
        return resolveSession.getClassDescriptor(classOrObject);
    }

    @NotNull
    @Override
    public BindingContext getBindingContext() {
        return resolveSession.getBindingContext();
    }

    @NotNull
    @Override
    public DeclarationDescriptor resolveToDescriptor(JetDeclaration declaration) {
        return resolveSession.resolveToDescriptor(declaration);
    }

    @Override
    public void forceResolveAll() {
        resolveSession.forceResolveAll();
    }

    @Override
    public long getModificationCount() {
        return resolveSession.getExceptionTracker().getModificationCount();
    }

    @Override
    public String toString() {
        return "ResolveSessionForBodies: " + getModificationCount() + " " + createdForObject + " " + createdForObject.hashCode();
    }
}
