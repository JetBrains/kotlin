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

package org.jetbrains.kotlin.resolve.lazy;

import com.intellij.psi.PsiElement;
import kotlin.Function0;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.ImportPath;
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull;
import org.jetbrains.kotlin.storage.NotNullLazyValue;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ScopeProvider {
    public static class AdditionalFileScopeProvider {
        @NotNull
        public List<JetScope> scopes(@NotNull JetFile file) {
            return Collections.emptyList();
        }
    }

    private final ResolveSession resolveSession;

    private final NotNullLazyValue<Collection<JetImportDirective>> defaultImports;

    private final MemoizedFunctionToNotNull<JetFile, LazyFileScope> fileScopes;

    @SuppressWarnings("ConstantConditions") @NotNull
    private AdditionalFileScopeProvider additionalFileScopeProvider = null;

    @Inject
    public void setAdditionalFileScopesProvider(@NotNull AdditionalFileScopeProvider additionalFileScopeProvider) {
        this.additionalFileScopeProvider = additionalFileScopeProvider;
    }

    public ScopeProvider(@NotNull final ResolveSession resolveSession) {
        this.resolveSession = resolveSession;

        this.defaultImports = resolveSession.getStorageManager().createLazyValue(new Function0<Collection<JetImportDirective>>() {
            @Override
            public Collection<JetImportDirective> invoke() {
                PackageViewDescriptor rootPackage = resolveSession.getModuleDescriptor().getPackage(FqName.ROOT);
                if (rootPackage == null) {
                    throw new IllegalStateException("Root package not found");
                }

                JetImportsFactory importsFactory = resolveSession.getJetImportsFactory();
                List<ImportPath> defaultImports = resolveSession.getModuleDescriptor().getDefaultImports();

                return importsFactory.createImportDirectives(defaultImports);
            }
        });


        this.fileScopes = resolveSession.getStorageManager().createMemoizedFunction(new Function1<JetFile, LazyFileScope>() {
            @Override
            public LazyFileScope invoke(JetFile file) {
                return createFileScope(file);
            }
        });
    }

    @NotNull
    public LazyFileScope getFileScope(@NotNull JetFile file) {
        return fileScopes.invoke(file);
    }

    private LazyFileScope createFileScope(@NotNull JetFile file) {
        TemporaryBindingTrace tempTrace = TemporaryBindingTrace.create(resolveSession.getTrace(), "Transient trace for default imports lazy resolve");
        return LazyFileScope.OBJECT$.create(
                resolveSession,
                file,
                defaultImports.invoke(),
                additionalFileScopeProvider.scopes(file),
                resolveSession.getTrace(),
                tempTrace,
                "LazyFileScope for file " + file.getName());
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
