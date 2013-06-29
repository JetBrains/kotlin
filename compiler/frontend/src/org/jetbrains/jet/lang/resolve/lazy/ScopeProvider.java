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
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNotNull;
import org.jetbrains.jet.lang.resolve.lazy.storage.NotNullLazyValue;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.ChainedScope;
import org.jetbrains.jet.lang.resolve.scopes.InnerClassesScopeWrapper;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager.ReferenceKind.WEAK;

public class ScopeProvider {
    private final ResolveSession resolveSession;

    private final MemoizedFunctionToNotNull<JetFile, JetScope> fileScopes;

    private final NotNullLazyValue<JetScope> defaultImportsScope;

    public ScopeProvider(@NotNull ResolveSession resolveSession) {
        this.resolveSession = resolveSession;

        this.fileScopes = resolveSession.getStorageManager().createMemoizedFunction(new Function<JetFile, JetScope>() {
            @Override
            public JetScope fun(@NotNull JetFile file) {
                return createFileScope(file);
            }
        }, WEAK);

        this.defaultImportsScope = resolveSession.getStorageManager().createLazyValue(new Computable<JetScope>() {
            @Override
            public JetScope compute() {
                return createScopeWithDefaultImports();
            }
        });
    }

    @NotNull
    public JetScope getFileScope(JetFile file) {
        return fileScopes.fun(file);
    }

    private JetScope createFileScope(JetFile file) {
        NamespaceDescriptor rootPackageDescriptor = resolveSession.getPackageDescriptorByFqName(FqName.ROOT);
        if (rootPackageDescriptor == null) {
            throw new IllegalStateException("Root package not found");
        }

        NamespaceDescriptor packageDescriptor = getFilePackageDescriptor(file);

        JetScope importsScope = LazyImportScope.createImportScopeForFile(
                resolveSession,
                packageDescriptor,
                file,
                resolveSession.getTrace(),
                "Lazy Imports Scope for file " + file.getName());

        return new ChainedScope(packageDescriptor,
                                "File scope: " + file.getName(),
                                packageDescriptor.getMemberScope(),
                                rootPackageDescriptor.getMemberScope(),
                                importsScope,
                                defaultImportsScope.compute());
    }

    private JetScope createScopeWithDefaultImports() {
        NamespaceDescriptor rootPackageDescriptor = resolveSession.getPackageDescriptorByFqName(FqName.ROOT);
        if (rootPackageDescriptor == null) {
            throw new IllegalStateException("Root package not found");
        }

        JetImportsFactory importsFactory = resolveSession.getInjector().getJetImportsFactory();
        List<ImportPath> defaultImports = resolveSession.getRootModuleDescriptor().getDefaultImports();

        Collection<JetImportDirective> defaultImportDirectives = importsFactory.createImportDirectives(defaultImports);

        return new LazyImportScope(
                resolveSession,
                rootPackageDescriptor,
                Lists.reverse(Lists.newArrayList(defaultImportDirectives)),
                TemporaryBindingTrace.create(resolveSession.getTrace(), "Transient trace for default imports lazy resolve"),
                "Lazy default imports scope");
    }

    @NotNull
    private NamespaceDescriptor getFilePackageDescriptor(JetFile file) {
        JetNamespaceHeader header = file.getNamespaceHeader();
        if (header == null) {
            throw new IllegalArgumentException("Scripts are not supported: " + file.getName());
        }

        FqName fqName = new FqName(header.getQualifiedName());
        NamespaceDescriptor packageDescriptor = resolveSession.getPackageDescriptorByFqName(fqName);

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

        assert jetDeclaration != null : "Can't happen because of getParentOfType(null, ?) == null";

        if (parentDeclaration instanceof JetClassOrObject) {
            JetClassOrObject classOrObject = (JetClassOrObject) parentDeclaration;
            LazyClassDescriptor classDescriptor = (LazyClassDescriptor) resolveSession.getClassDescriptor(classOrObject);
            if (jetDeclaration instanceof JetClassInitializer || jetDeclaration instanceof JetProperty) {
                return classDescriptor.getScopeForPropertyInitializerResolution();
            }
            if (jetDeclaration instanceof JetEnumEntry) {
                LazyClassDescriptor descriptor = (LazyClassDescriptor) classDescriptor.getClassObjectDescriptor();
                assert descriptor != null : "There should be class object descriptor for enum class " + parentDeclaration.getText() +
                                            " on entry " + jetDeclaration.getText();

                return descriptor.getScopeForMemberDeclarationResolution();
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