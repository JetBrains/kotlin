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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.di.InjectorForLazyResolve;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class ResolveSession {
    private final ModuleDescriptor module;
    private final LazyPackageDescriptor rootPackage;

    private final BindingTrace trace = new BindingTraceContext();
    private final DeclarationProviderFactory declarationProviderFactory;

    private final InjectorForLazyResolve injector;

    public ResolveSession(
            @NotNull Project project,
            @NotNull ModuleDescriptor rootDescriptor,
            @NotNull DeclarationProviderFactory declarationProviderFactory
    ) {
        this.injector = new InjectorForLazyResolve(project, this, trace);
        this.module = rootDescriptor;
        PackageMemberDeclarationProvider provider = declarationProviderFactory.getPackageMemberDeclarationProvider(FqName.ROOT);
        assert provider != null : "No declaration provider for root package in " + rootDescriptor;
        this.rootPackage = new LazyPackageDescriptor(rootDescriptor, FqNameUnsafe.ROOT_NAME, this, provider);
        rootDescriptor.setRootNamespace(rootPackage);

        this.declarationProviderFactory = declarationProviderFactory;
    }

    @NotNull
    public InjectorForLazyResolve getInjector() {
        return injector;
    }

    @Nullable
    public NamespaceDescriptor getPackageDescriptor(@NotNull Name shortName) {
        return rootPackage.getMemberScope().getNamespace(shortName);
    }

    @Nullable
    public NamespaceDescriptor getPackageDescriptorByFqName(FqName fqName) {
        if (fqName.isRoot()) {
            return rootPackage;
        }
        List<Name> names = fqName.pathSegments();
        NamespaceDescriptor current = getPackageDescriptor(names.get(0));
        if (current == null) return null;
        for (Name name : names.subList(1, names.size())) {
            current = current.getMemberScope().getNamespace(name);
            if (current == null) return null;
        }
        return current;
    }

    @NotNull
    public ClassDescriptor getClassDescriptor(@NotNull JetClassOrObject classOrObject) {
        JetScope resolutionScope = getInjector().getScopeProvider().getResolutionScopeForDeclaration((JetDeclaration) classOrObject);
        ClassifierDescriptor classifier = resolutionScope.getClassifier(classOrObject.getNameAsName());
        return (ClassDescriptor) classifier;
    }

    public Collection<DeclarationDescriptor> getDescriptorsForDeclarations(Collection<PsiElement> declarationsOrFiles) {
        final List<DeclarationDescriptor> descriptors = Lists.newArrayList();
        for (PsiElement declarationOrFile : declarationsOrFiles) {
            declarationOrFile.accept(new JetVisitorVoid() {
                @Override
                public void visitJetFile(JetFile file) {
                    JetNamespaceHeader header = file.getNamespaceHeader();
                    if (header == null) {
                        throw new UnsupportedOperationException("Lazy resolve is not supported for scripts");
                    }
                    NamespaceDescriptor packageDescriptor = getPackageDescriptorByFqName(new FqName(header.getQualifiedName()));
                    if (packageDescriptor == null) {
                        throw new IllegalStateException("Package descriptor not found for: " + header.getQualifiedName());
                    }
                    JetScope packageMemberScope = packageDescriptor.getMemberScope();
                    for (JetDeclaration declaration : file.getDeclarations()) {
                        collectDescriptors(packageMemberScope, declaration);
                    }
                }

                @Override
                public void visitDeclaration(JetDeclaration dcl) {
                    JetScope scope = injector.getScopeProvider().getResolutionScopeForDeclaration(dcl);
                    collectDescriptors(scope, dcl);
                }

                private void collectDescriptors(JetScope outerScope, JetDeclaration declaration) {
                    if (declaration instanceof JetClass) {
                        JetClass jetClass = (JetClass) declaration;
                        descriptors.add(outerScope.getClassifier(jetClass.getNameAsSafeName()));
                    }
                    else if (declaration instanceof JetFunction) {
                        JetFunction jetFunction = (JetFunction) declaration;
                        Set<FunctionDescriptor> functionDescriptors = outerScope.getFunctions(jetFunction.getNameAsSafeName());
                        descriptors.addAll(functionDescriptors);
                    }
                    else if (declaration instanceof JetProperty) {
                        JetProperty jetProperty = (JetProperty) declaration;
                        Set<VariableDescriptor> functionDescriptors = outerScope.getProperties(jetProperty.getNameAsSafeName());
                        descriptors.addAll(functionDescriptors);
                    }
                    else if (declaration instanceof JetObjectDeclaration) {
                        JetObjectDeclaration jetObjectDeclaration = (JetObjectDeclaration) declaration;
                        descriptors.addAll(outerScope.getProperties(jetObjectDeclaration.getNameAsSafeName()));
                        descriptors.add(outerScope.getObjectDescriptor(jetObjectDeclaration.getNameAsSafeName()));
                    }
                }
            });
        }
        return descriptors;
    }

    @NotNull
    public BindingContext getBindingContext() {
        return trace.getBindingContext();
    }

    @NotNull
    /*package*/ BindingTrace getTrace() {
        return trace;
    }

    @NotNull
    public DeclarationProviderFactory getDeclarationProviderFactory() {
        return declarationProviderFactory;
    }

    @NotNull
    public JetScope getResolutionScope(PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent instanceof JetFile) {
            JetFile file = (JetFile) parent;
            return getInjector().getScopeProvider().getFileScopeForDeclarationResolution(file);
        }

        if (parent instanceof JetClassBody) {
            JetClassBody classBody = (JetClassBody) parent;
            JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(classBody, JetClassOrObject.class);
            ClassDescriptor classDescriptor = getClassDescriptor(classOrObject);
            assert classDescriptor instanceof LazyClassDescriptor : "Trying to resolve a member of a non-lazily loaded class: " + element;
            return ((LazyClassDescriptor) classDescriptor).getScopeForMemberDeclarationResolution();
        }

        throw new IllegalArgumentException("Unsupported PSI element: " + element);
    }
}
