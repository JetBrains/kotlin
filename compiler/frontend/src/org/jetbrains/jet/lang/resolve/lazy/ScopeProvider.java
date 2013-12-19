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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import jet.Function0;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.ChainedScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.storage.MemoizedFunctionToNotNull;
import org.jetbrains.jet.storage.NotNullLazyValue;

import java.util.Collection;
import java.util.List;

public class ScopeProvider {
    private final ResolveSession resolveSession;

    private final MemoizedFunctionToNotNull<JetFile, JetScope> fileScopes;

    private final NotNullLazyValue<JetScope> defaultImportsScope;

    public ScopeProvider(@NotNull ResolveSession resolveSession) {
        this.resolveSession = resolveSession;

        this.fileScopes = resolveSession.getStorageManager().createWeaklyRetainedMemoizedFunction(new Function1<JetFile, JetScope>() {
            @Override
            public JetScope invoke(@NotNull JetFile file) {
                return createFileScope(file);
            }
        });

        this.defaultImportsScope = resolveSession.getStorageManager().createLazyValue(new Function0<JetScope>() {
            @Override
            public JetScope invoke() {
                return createScopeWithDefaultImports();
            }
        });
    }

    @NotNull
    public JetScope getFileScope(JetFile file) {
        return fileScopes.invoke(file);
    }

    private JetScope createFileScope(JetFile file) {
        PackageViewDescriptor rootPackageDescriptor = resolveSession.getModuleDescriptor().getPackage(FqName.ROOT);
        if (rootPackageDescriptor == null) {
            throw new IllegalStateException("Root package not found");
        }

        PackageViewDescriptor packageDescriptor = getFilePackageDescriptor(file);

        JetScope importsScope = LazyImportScope.createImportScopeForFile(
                resolveSession,
                packageDescriptor,
                file,
                resolveSession.getTrace(),
                "Lazy Imports Scope for file " + file.getName());

        return new ChainedScope(resolveSession.getPackageFragment(JetPsiUtil.getFQName(file)),
                                "File scope: " + file.getName(),
                                packageDescriptor.getMemberScope(),
                                rootPackageDescriptor.getMemberScope(),
                                importsScope,
                                defaultImportsScope.invoke());
    }

    private JetScope createScopeWithDefaultImports() {
        PackageViewDescriptor rootPackage = resolveSession.getModuleDescriptor().getPackage(FqName.ROOT);
        if (rootPackage == null) {
            throw new IllegalStateException("Root package not found");
        }

        JetImportsFactory importsFactory = resolveSession.getInjector().getJetImportsFactory();
        List<ImportPath> defaultImports = resolveSession.getModuleDescriptor().getDefaultImports();

        Collection<JetImportDirective> defaultImportDirectives = importsFactory.createImportDirectives(defaultImports);

        return new LazyImportScope(
                resolveSession,
                rootPackage,
                Lists.reverse(Lists.newArrayList(defaultImportDirectives)),
                TemporaryBindingTrace.create(resolveSession.getTrace(), "Transient trace for default imports lazy resolve"),
                "Lazy default imports scope");
    }

    @NotNull
    private PackageViewDescriptor getFilePackageDescriptor(JetFile file) {
        JetNamespaceHeader header = file.getNamespaceHeader();
        if (header == null) {
            throw new IllegalArgumentException("Scripts are not supported: " + file.getName());
        }

        FqName fqName = new FqName(header.getQualifiedName());
        PackageViewDescriptor packageDescriptor = resolveSession.getModuleDescriptor().getPackage(fqName);

        if (packageDescriptor == null) {
            throw new IllegalStateException("Package not found: " + fqName + " maybe the file is not in scope of this resolve session: " + file.getName());
        }

        return packageDescriptor;
    }

    @NotNull
    public JetScope getResolutionScopeForDeclaration(@NotNull PsiElement elementOfDeclaration) {
        JetDeclaration jetDeclaration = PsiTreeUtil.getParentOfType(elementOfDeclaration, JetDeclaration.class, false);

        assert !(elementOfDeclaration instanceof JetDeclaration) || jetDeclaration == elementOfDeclaration :
                "For JetDeclaration element getParentOfType() should return itself.";

        JetDeclaration parentDeclaration = PsiTreeUtil.getParentOfType(jetDeclaration, JetDeclaration.class);
        if (parentDeclaration == null) {
            return getFileScope((JetFile) elementOfDeclaration.getContainingFile());
        }

        if (parentDeclaration instanceof JetClassOrObject) {
            JetClassOrObject classOrObject = (JetClassOrObject) parentDeclaration;
            LazyClassDescriptor classDescriptor = (LazyClassDescriptor) resolveSession.getClassDescriptor(classOrObject);
            if (jetDeclaration instanceof JetClassInitializer || jetDeclaration instanceof JetProperty) {
                return classDescriptor.getScopeForPropertyInitializerResolution();
            }
            return classDescriptor.getScopeForMemberDeclarationResolution();
        }

        if (parentDeclaration instanceof JetClassObject) {
            assert jetDeclaration instanceof JetObjectDeclaration : "Should be situation for getting scope for object in class [object {...}]";

            JetClassObject classObject = (JetClassObject) parentDeclaration;
            LazyClassDescriptor classObjectDescriptor =
                    (LazyClassDescriptor) resolveSession.getClassObjectDescriptor(classObject).getContainingDeclaration();

            return classObjectDescriptor.getScopeForMemberDeclarationResolution();
        }

        throw new IllegalStateException("Don't call this method for local declarations: " + jetDeclaration + " " + jetDeclaration.getText());
    }
}