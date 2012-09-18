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
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.ImportsResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.*;

/**
 * @author abreslav
 */
public class ScopeProvider {

    private final ResolveSession resolveSession;

    public ScopeProvider(@NotNull ResolveSession resolveSession) {
        this.resolveSession = resolveSession;
    }

    // This scope does not contain imported functions
    @NotNull
    public JetScope getFileScopeForDeclarationResolution(JetFile file) {
        // package
        JetNamespaceHeader header = file.getNamespaceHeader();
        if (header == null) {
            throw new IllegalArgumentException("Scripts are not supported: " + file.getName());
        }

        FqName fqName = new FqName(header.getQualifiedName());
        NamespaceDescriptor packageDescriptor = resolveSession.getPackageDescriptorByFqName(fqName);

        if (packageDescriptor == null) {
            throw new IllegalStateException("Package not found: " + fqName + " maybe the file is not in scope of this resolve session: " + file.getName());
        }

        WritableScope fileScope = new WritableScopeImpl(
                JetScope.EMPTY, packageDescriptor, RedeclarationHandler.DO_NOTHING, "File scope for declaration resolution");

        fileScope.changeLockLevel(WritableScope.LockLevel.BOTH);

        NamespaceDescriptor rootPackageDescriptor = resolveSession.getPackageDescriptorByFqName(FqName.ROOT);
        if (rootPackageDescriptor == null) {
            throw new IllegalStateException("Root package not found");
        }

        // Don't import twice
        if (!packageDescriptor.getQualifiedName().equals(FqName.ROOT)) {
            fileScope.importScope(rootPackageDescriptor.getMemberScope());
        }

        ImportsResolver.processImportsInFile(true, fileScope, Lists.newArrayList(file.getImportDirectives()),
                                             rootPackageDescriptor.getMemberScope(),
                                             resolveSession.getModuleConfiguration(), resolveSession.getTrace(),
                                             resolveSession.getInjector().getQualifiedExpressionResolver());

        fileScope.changeLockLevel(WritableScope.LockLevel.READING);

        // TODO: Cache
        return new ChainedScope(packageDescriptor, packageDescriptor.getMemberScope(), fileScope);
    }

    @NotNull
    public JetScope getResolutionScopeForDeclaration(@NotNull PsiElement elementOfDeclaration) {
        JetDeclaration jetDeclaration = PsiTreeUtil.getParentOfType(elementOfDeclaration, JetDeclaration.class, false);

        assert !(elementOfDeclaration instanceof JetDeclaration) || jetDeclaration == elementOfDeclaration :
                "For JetDeclaration element getParentOfType() should return itself.";

        JetDeclaration parentDeclaration = PsiTreeUtil.getParentOfType(jetDeclaration, JetDeclaration.class);
        if (parentDeclaration == null) {
            return getFileScopeForDeclarationResolution((JetFile) elementOfDeclaration.getContainingFile());
        }

        assert jetDeclaration != null : "Can't happen because of getParentOfType(null, ?) == null";

        if (parentDeclaration instanceof JetClassOrObject) {
            JetClassOrObject classOrObject = (JetClassOrObject) parentDeclaration;
            LazyClassDescriptor classDescriptor = (LazyClassDescriptor) resolveSession.getClassDescriptor(classOrObject);
            if (jetDeclaration instanceof JetClassInitializer || jetDeclaration instanceof JetProperty) {
                return classDescriptor.getScopeForPropertyInitializerResolution();
            }
            if (jetDeclaration instanceof JetEnumEntry) {
                return ((LazyClassDescriptor) classDescriptor.getClassObjectDescriptor()).getScopeForMemberDeclarationResolution();
            }
            return classDescriptor.getScopeForMemberDeclarationResolution();
        }

        if (parentDeclaration instanceof JetClassObject) {
            assert jetDeclaration instanceof JetObjectDeclaration : "Should be situation for getting scope for object in class [object {...}]";

            JetClassObject classObject = (JetClassObject) parentDeclaration;
            LazyClassDescriptor classObjectDescriptor =
                    (LazyClassDescriptor) resolveSession.getClassObjectDescriptor(classObject).getContainingDeclaration();

            // During class object header resolve there should be no resolution for parent class generic params
            return new InnerClassesScopeWrapper(classObjectDescriptor.getScopeForMemberDeclarationResolution());
        }

        throw new IllegalStateException("Don't call this method for local declarations: " + jetDeclaration + " " + jetDeclaration.getText());
    }
}