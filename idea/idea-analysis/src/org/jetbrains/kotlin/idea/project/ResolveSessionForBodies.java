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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.kotlin.resolve.lazy.ResolveSession;
import org.jetbrains.kotlin.resolve.lazy.ScopeProvider;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor;
import org.jetbrains.kotlin.storage.ExceptionTracker;
import org.jetbrains.kotlin.storage.StorageManager;

import java.util.Collection;

public class ResolveSessionForBodies implements KotlinCodeAnalyzer {
    private final Object createdForObject;
    private final ResolveSession resolveSession;
    private final ResolveElementCache resolveElementCache;

    public ResolveSessionForBodies(@NotNull Project project, @NotNull ResolveSession resolveSession) {
        this(project, project, resolveSession);
    }

    private ResolveSessionForBodies(Object createdForObject, Project project, ResolveSession resolveSession) {
        this.createdForObject = createdForObject;
        this.resolveSession = resolveSession;
        this.resolveElementCache = new ResolveElementCache(resolveSession, project);
    }

    @NotNull
    public BindingContext resolveToElement(JetElement element, BodyResolveMode bodyResolveMode) {
        return resolveElementCache.resolveToElement(element, bodyResolveMode);
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
    public DeclarationDescriptor resolveToDescriptor(@NotNull JetDeclaration declaration) {
        if (!JetPsiUtil.isLocal(declaration)) {
            return resolveSession.resolveToDescriptor(declaration);
        }

        BindingContext context = resolveElementCache.resolveToElement(declaration, BodyResolveMode.FULL);
        return BindingContextUtils.getNotNull(context, BindingContext.DECLARATION_TO_DESCRIPTOR, declaration,
                                              "Descriptor wasn't found for declaration " + declaration.toString() + "\n" +
                                              PsiUtilPackage.getElementTextWithContext(declaration));
    }

    @Override
    public void forceResolveAll() {
        resolveSession.forceResolveAll();
    }

    @Override
    public String toString() {
        return "ResolveSessionForBodies: " + resolveSession.getExceptionTracker().getModificationCount()
               + " " + createdForObject + " " + createdForObject.hashCode();
    }

    @Override
    @Nullable
    public LazyPackageDescriptor getPackageFragment(@NotNull FqName fqName) {
        return resolveSession.getPackageFragment(fqName);
    }

    @Override
    @NotNull
    @ReadOnly
    public Collection<ClassDescriptor> getTopLevelClassDescriptors(@NotNull FqName fqName) {
        return resolveSession.getTopLevelClassDescriptors(fqName);
    }

    @Override
    @NotNull
    public ScriptDescriptor getScriptDescriptor(@NotNull JetScript script) {
        return resolveSession.getScriptDescriptor(script);
    }

    @Override
    @NotNull
    public ScopeProvider getScopeProvider() {
        return resolveSession.getScopeProvider();
    }

    @NotNull
    public StorageManager getStorageManager() {
        return resolveSession.getStorageManager();
    }

    @NotNull
    public ExceptionTracker getExceptionTracker() {
        return resolveSession.getExceptionTracker();
    }

    @NotNull
    @Override
    public PackageFragmentProvider getPackageFragmentProvider() {
        return resolveSession.getPackageFragmentProvider();
    }

    @NotNull
    public QualifiedExpressionResolver getQualifiedExpressionResolver() {
        return resolveSession.getQualifiedExpressionResolver();
    }
}
