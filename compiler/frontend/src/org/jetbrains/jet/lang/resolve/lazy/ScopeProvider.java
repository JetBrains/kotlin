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

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
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

        WritableScope writableScope = new WritableScopeImpl(
                JetScope.EMPTY, packageDescriptor, RedeclarationHandler.DO_NOTHING, "file scope for declaration resollution");
        writableScope.importScope(resolveSession.getPackageDescriptorByFqName(FqName.ROOT).getMemberScope());
        writableScope.importScope(packageDescriptor.getMemberScope());

        // TODO: imports

        writableScope.changeLockLevel(WritableScope.LockLevel.READING);
        // TODO: Cache
        return writableScope;
    }

    @NotNull
    public JetScope getScopeForClassMemberResolution(@NotNull JetClassOrObject classOrObject) {
        // TODO: cache
        ClassDescriptor classDescriptor = resolveSession.getClassDescriptor(classOrObject);
        JetScope memberScope = classDescriptor.getDefaultType().getMemberScope();
        JetScope outerScope = getResolutionScopeForDeclaration((JetDeclaration) classOrObject);

        WritableScope typeParametersScope = new WritableScopeImpl(
                JetScope.EMPTY, classDescriptor, RedeclarationHandler.DO_NOTHING, "scope for class member resolution");
        for (TypeParameterDescriptor typeParameterDescriptor : classDescriptor.getTypeConstructor().getParameters()) {
            typeParametersScope.addClassifierDescriptor(typeParameterDescriptor);
        }

        return new ChainedScope(classDescriptor, memberScope, typeParametersScope, outerScope);
    }

    public JetScope getScopeForClassSupertypeResolution(JetClassOrObject declaration) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    public JetScope getResolutionScopeForDeclaration(@NotNull JetDeclaration jetDeclaration) {
        PsiElement immediateParent = jetDeclaration.getParent();
        if (immediateParent instanceof JetFile) {
            return getFileScopeForDeclarationResolution((JetFile) immediateParent);
        }

        JetDeclaration parentDeclaration = PsiTreeUtil.getParentOfType(jetDeclaration, JetDeclaration.class);
        if (parentDeclaration instanceof JetClassOrObject) {
            JetClassOrObject classOrObject = (JetClassOrObject) parentDeclaration;
            return getScopeForClassMemberResolution(classOrObject);
        }
        else {
            throw new IllegalStateException("Don't call this method for local declarations: " + jetDeclaration);
        }
    }

    public ClassDescriptor buildLazyClassDescriptor(DeclarationDescriptor declaration, Name name, JetScope outerScope) {
        throw new UnsupportedOperationException(); // TODO
    }

    public NamespaceDescriptor buildLazyPackageDescriptor(DeclarationDescriptor declaration,
            Name name) {
        throw new UnsupportedOperationException(); // TODO
    }
}
