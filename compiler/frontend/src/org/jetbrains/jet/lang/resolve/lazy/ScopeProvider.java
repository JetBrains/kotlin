/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import kotlin.Function0;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.JetModuleUtil;
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

    private final MemoizedFunctionToNotNull<JetFile, LazyImportScope> explicitImportScopes;

    private final NotNullLazyValue<JetScope> defaultImportsScope;

    public ScopeProvider(@NotNull ResolveSession resolveSession) {
        this.resolveSession = resolveSession;

        this.explicitImportScopes = resolveSession.getStorageManager().createMemoizedFunction(new Function1<JetFile, LazyImportScope>() {
            @Override
            public LazyImportScope invoke(@NotNull JetFile file) {
                return createExplicitImportScope(file);
            }
        });

        this.defaultImportsScope = resolveSession.getStorageManager().createLazyValue(new Function0<JetScope>() {
            @Override
            public JetScope invoke() {
                return createScopeWithDefaultImports();
            }
        });
    }

    private LazyImportScope createExplicitImportScope(@NotNull JetFile file) {
        return LazyImportScope.createImportScopeForFile(
                resolveSession,
                getFilePackageDescriptor(file),
                file,
                resolveSession.getTrace(),
                "Lazy Imports Scope for file " + file.getName());
    }

    @NotNull
    public JetScope getFileScope(@NotNull JetFile file) {
        return new ChainedScope(resolveSession.getPackageFragment(file.getPackageFqName()),
                                "File scope: " + file.getName(),
                                getFilePackageDescriptor(file).getMemberScope(),
                                JetModuleUtil.getSubpackagesOfRootScope(resolveSession.getModuleDescriptor()),
                                explicitImportScopes.invoke(file),
                                defaultImportsScope.invoke());
    }

    @NotNull
    public LazyImportScope getExplicitImportsScopeForFile(@NotNull JetFile file) {
        return explicitImportScopes.invoke(file);
    }

    private JetScope createScopeWithDefaultImports() {
        PackageViewDescriptor rootPackage = resolveSession.getModuleDescriptor().getPackage(FqName.ROOT);
        if (rootPackage == null) {
            throw new IllegalStateException("Root package not found");
        }

        JetImportsFactory importsFactory = resolveSession.getJetImportsFactory();
        List<ImportPath> defaultImports = resolveSession.getModuleDescriptor().getDefaultImports();

        Collection<JetImportDirective> defaultImportDirectives = importsFactory.createImportDirectives(defaultImports);

        return new LazyImportScope(
                resolveSession,
                rootPackage,
                Lists.reverse(Lists.newArrayList(defaultImportDirectives)),
                TemporaryBindingTrace.create(resolveSession.getTrace(), "Transient trace for default imports lazy resolve"),
                "Lazy default imports scope",
                false);
    }

    @NotNull
    private PackageViewDescriptor getFilePackageDescriptor(JetFile file) {
        FqName fqName = file.getPackageFqName();
        PackageViewDescriptor packageDescriptor = resolveSession.getModuleDescriptor().getPackage(fqName);

        if (packageDescriptor == null) {
            throw new IllegalStateException("Package not found: " + fqName + " maybe the file is not in scope of this resolve session: " + file.getName());
        }

        return packageDescriptor;
    }

    @NotNull
    public JetScope getResolutionScopeForDeclaration(@NotNull PsiElement elementOfDeclaration) {
        JetDeclaration jetDeclaration = JetStubbedPsiUtil.getPsiOrStubParent(elementOfDeclaration, JetDeclaration.class, false);

        assert !(elementOfDeclaration instanceof JetDeclaration) || jetDeclaration == elementOfDeclaration :
                "For JetDeclaration element getParentOfType() should return itself.";
        assert jetDeclaration != null : "Should be contained inside declaration.";

        JetDeclaration parentDeclaration = JetStubbedPsiUtil.getContainingDeclaration(jetDeclaration);

        if (jetDeclaration instanceof JetPropertyAccessor) {
            parentDeclaration = JetStubbedPsiUtil.getContainingDeclaration(parentDeclaration, JetDeclaration.class);
        }

        if (parentDeclaration == null) {
            return getFileScope((JetFile) elementOfDeclaration.getContainingFile());
        }

        if (parentDeclaration instanceof JetClassOrObject) {
            JetClassOrObject classOrObject = (JetClassOrObject) parentDeclaration;
            LazyClassDescriptor classDescriptor = (LazyClassDescriptor) resolveSession.getClassDescriptor(classOrObject);
            if (jetDeclaration instanceof JetClassInitializer || jetDeclaration instanceof JetProperty) {
                return classDescriptor.getScopeForInitializerResolution();
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

        throw new IllegalStateException("Don't call this method for local declarations: " + jetDeclaration + "\n" +
                                        JetPsiUtil.getElementTextWithContext(jetDeclaration));
    }
}